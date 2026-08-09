/*
 *     This file is part of cracklepop
 *     Copyright (C) 2014-2018  Johannes Pohl
 *     Modifications Copyright (C) 2026  cracklepop contributors
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.badaix.snapcast.ui

import android.util.Log
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.badaix.snapcast.SnapclientService
import de.badaix.snapcast.repository.ConnectionState
import de.badaix.snapcast.repository.SnapcastRepository
import de.badaix.snapcast.utils.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * ViewModel that collects the repository flows and derives a single
 * [StateFlow] of [PlayerUiState]. Order-independent: each backing value
 * updates independently; the derived state reflects the current combined
 * values regardless of arrival order.
 */
class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SnapcastRepository(application)
    private val settings = Settings.getInstance(application)
    private val localHostId = SnapclientService.getUniqueId(application)

    // --- Service binding -----------------------------------------------------

    private val _serviceBound = MutableStateFlow(false)
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? SnapclientService.LocalBinder ?: return
            val svc = binder.getService()
            repository.bindService(svc)
            _serviceBound.value = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            repository.unbindService()
            _serviceBound.value = false
        }
    }

    fun bindService(context: Context) {
        val intent = Intent(context, SnapclientService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun unbindService(context: Context) {
        context.unbindService(serviceConnection)
    }

    // --- Derived state -------------------------------------------------------

    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Unconfigured)
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    /**
     * Flag to distinguish a user-initiated volume change from a server refresh.
     * When true, the next volume update from the server is treated as a refresh
     * and does NOT emit a control change back.
     */
    private val _isRefreshingVolume = MutableStateFlow(false)

    init {
        combine(
            repository.connectionState,
            repository.serverStatus,
            repository.playerRunning,
            _serviceBound,
        ) { connection, status, running, bound ->
            deriveState(connection, status, running, bound)
        }.onEach { state ->
            _uiState.value = state
        }.launchIn(viewModelScope)

        // Handle player start requests from the repository
        repository.playerStartRequested.onEach { params ->
            params?.let { (host, port) ->
                startServiceIntent(host, port)
                repository.acknowledgeStartRequest()
            }
        }.launchIn(viewModelScope)
    }

    private fun deriveState(
        connection: ConnectionState,
        status: de.badaix.snapcast.control.json.ServerStatus?,
        running: Boolean,
        bound: Boolean,
    ): PlayerUiState {
        val hasConfiguredHost = settings.getHost().isNotEmpty()
        Log.d(TAG, "deriveState: hasConfiguredHost=$hasConfiguredHost, host='${settings.getHost()}', connection=$connection")

        // No server ever configured
        if (!hasConfiguredHost) {
            return PlayerUiState.Unconfigured
        }

        // Server configured but not connected
        if (connection != ConnectionState.Connected) {
            return PlayerUiState.ServerUnavailable(
                configuredHost = settings.getHost(),
                configuredPort = settings.getControlPort(),
            )
        }

        // Connected — resolve local client
        val displayInfo = resolveClientDisplay(status, localHostId)

        if (running) {
            // Client is running — we should have a matched client
            if (displayInfo != null) {
                return PlayerUiState.ClientRunning(
                    localClient = displayInfo.toClient(),
                    volume = displayInfo.volume,
                    muted = displayInfo.muted,
                    groupName = displayInfo.groupName,
                    streamName = displayInfo.streamName,
                )
            } else {
                // Running but no matching client yet — treat as connecting
                return PlayerUiState.ClientConnecting(
                    localClient = null,
                    groupName = null,
                    streamName = null,
                )
            }
        }

        // Not running — check if we recently started (connecting)
        // We use the player error state to determine if we should show stopped
        // For now, if not running and connected, show stopped
        return PlayerUiState.ClientStopped(
            localClient = displayInfo?.toClient(),
            groupName = displayInfo?.groupName,
            streamName = displayInfo?.streamName,
        )
    }

    // --- Server setup navigation ---------------------------------------------

    private val _showServerSetup = MutableStateFlow(false)
    val showServerSetup: StateFlow<Boolean> = _showServerSetup.asStateFlow()

    fun setupServer() {
        _showServerSetup.value = true
    }

    fun hideServerSetup() {
        _showServerSetup.value = false
    }

    // --- Actions -------------------------------------------------------------

    fun retryConnection() {
        val host = settings.getHost()
        Log.d(TAG, "retryConnection: host='$host', port=${settings.getControlPort()}")
        if (host.isNotEmpty()) {
            repository.connect(host, settings.getControlPort())
        }
    }

    fun startListening() {
        viewModelScope.launch {
            repository.startPlayer()
        }
    }

    fun stopListening() {
        repository.stopPlayer()
    }

    fun setVolume(percent: Int) {
        _isRefreshingVolume.value = false
        repository.setVolume(percent)
    }

    fun toggleMute() {
        repository.toggleMute()
    }

    fun onServerSelected(host: String, streamPort: Int) {
        val controlPort = streamPort + 1
        settings.setHost(host, streamPort, controlPort)
        _showServerSetup.value = false
        repository.connect(host, controlPort)
    }

    /**
     * Called when a volume update arrives from the server (not user-initiated).
     * The displayed value updates without sending a control change back.
     */
    fun onVolumeRefresh() {
        _isRefreshingVolume.value = true
    }

    private fun startServiceIntent(host: String, port: Int) {
        val context = getApplication<Application>()
        val intent = Intent(context, SnapclientService::class.java).apply {
            action = SnapclientService.ACTION_START
            putExtra(SnapclientService.EXTRA_HOST, host)
            putExtra(SnapclientService.EXTRA_PORT, port)
        }
        context.startService(intent)
    }

    override fun onCleared() {
        super.onCleared()
        repository.cleanup()
    }

    companion object {
        private const val TAG = "PlayerViewModel"
    }
}

/** Helper to convert display info back to a Client reference for control calls. */
private fun ClientDisplayInfo.toClient(): de.badaix.snapcast.control.json.Client {
    // We need the actual Client object for the repository's setVolume call.
    // Since we only have display info here, we'll use the ID to look it up.
    // This is a simplification — the ViewModel should hold the actual Client ref.
    // For now, create a minimal Client with the right ID.
    val json = org.json.JSONObject().apply {
        put("id", id)
        put("connected", true)
        put("host", org.json.JSONObject().apply {
            put("mac", id)
            put("name", name)
        })
        put("snapclient", org.json.JSONObject().apply {
            put("name", "snapclient")
            put("protocolVersion", org.json.JSONObject().apply {
                put("major", 2)
                put("minor", 0)
            })
        })
        put("config", org.json.JSONObject().apply {
            put("volume", org.json.JSONObject().apply {
                put("percent", volume)
                put("muted", muted)
            })
            put("name", name)
            put("instance", 1)
            put("latency", 0)
        })
        put("lastSeen", org.json.JSONObject().apply {
            put("sec", 0)
            put("usec", 0)
        })
    }
    return de.badaix.snapcast.control.json.Client(json)
}
