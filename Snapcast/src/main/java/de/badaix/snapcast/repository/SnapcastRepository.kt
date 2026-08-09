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

package de.badaix.snapcast.repository

import android.content.Context
import android.util.Log
import de.badaix.snapcast.SnapclientService
import de.badaix.snapcast.control.RemoteControl
import de.badaix.snapcast.control.json.Client
import de.badaix.snapcast.control.json.Group
import de.badaix.snapcast.control.json.ServerStatus
import de.badaix.snapcast.control.json.Stream
import de.badaix.snapcast.control.json.Volume
import de.badaix.snapcast.utils.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Thin Kotlin adapter that owns [RemoteControl] and a bound [SnapclientService],
 * implements their listener interfaces, and republishes callbacks as [StateFlow]s.
 *
 * The ViewModel collects these flows and derives a single [PlayerUiState]; the
 * repository never touches views.
 */
class SnapcastRepository(private val appContext: Context) :
    RemoteControl.RemoteControlListener,
    SnapclientService.SnapclientListener {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val settings = Settings.getInstance(appContext)
    private val localHostId: String = SnapclientService.getUniqueId(appContext)

    private val remoteControl = RemoteControl(this)

    // --- Backing state -------------------------------------------------------

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _serverStatus = MutableStateFlow<ServerStatus?>(null)
    val serverStatus: StateFlow<ServerStatus?> = _serverStatus.asStateFlow()

    private val _playerRunning = MutableStateFlow(false)
    val playerRunning: StateFlow<Boolean> = _playerRunning.asStateFlow()

    private val _playerError = MutableStateFlow<String?>(null)
    val playerError: StateFlow<String?> = _playerError.asStateFlow()

    /** The local client resolved by exact host-ID match, or null. */
    val localClient: StateFlow<Client?> =
        combineLocalClient()

    // --- Service binding -----------------------------------------------------

    private var snapclientService: SnapclientService? = null

    /**
     * Call after binding to [SnapclientService]. Registers this repository as
     * the service listener.
     */
    fun bindService(service: SnapclientService) {
        snapclientService = service
        service.setListener(this)
    }

    fun unbindService() {
        snapclientService?.setListener(null)
        snapclientService = null
    }

    // --- Server connection ---------------------------------------------------

    /** Connect to the persisted server, or do nothing if none is configured. */
    fun connectToPersistedServer(): Boolean {
        val host = settings.getHost()
        if (host.isEmpty()) return false
        connect(host, settings.getControlPort())
        return true
    }

    fun connect(host: String, controlPort: Int) {
        settings.setHost(host, controlPort - 1, controlPort)
        remoteControl.connect(host, controlPort)
    }

    fun disconnect() {
        remoteControl.disconnect()
        _connectionState.value = ConnectionState.Disconnected
        _serverStatus.value = null
    }

    fun isConnected(): Boolean = remoteControl.isConnected()

    // --- Player control ------------------------------------------------------

    fun startPlayer() {
        val service = snapclientService ?: return
        val host = settings.getHost()
        val port = settings.getStreamPort()
        // The service expects an intent; we call via the public start method
        // by reflecting the internal start(host, port). Instead, use the
        // existing pattern: the service is started via an intent from the
        // Activity.  For the repository we expose a callback that the Activity
        // can use to fire the intent.
        // Actually, the service has a package-private start() — we cannot call
        // it directly.  The Activity must start the service with an intent.
        // We expose the host/port so the Activity can build the intent.
        _playerStartRequested.value = Pair(host, port)
    }

    fun stopPlayer() {
        snapclientService?.stopPlayer()
    }

    private val _playerStartRequested = MutableStateFlow<Pair<String, Int>?>(null)
    /** When non-null, the Activity should start the service with these params. */
    val playerStartRequested: StateFlow<Pair<String, Int>?> = _playerStartRequested.asStateFlow()

    fun acknowledgeStartRequest() {
        _playerStartRequested.value = null
    }

    // --- Volume / mute (per matched local client) ----------------------------

    fun setVolume(percent: Int) {
        val client = localClient.value ?: return
        val currentVolume = client.config.volume
        remoteControl.setVolume(client, percent, currentVolume.isMuted)
    }

    fun toggleMute() {
        val client = localClient.value ?: return
        val currentVolume = client.config.volume
        remoteControl.setVolume(client, currentVolume.percent, !currentVolume.isMuted)
    }

    // --- RemoteControl.RemoteControlListener ---------------------------------

    override fun onConnected(remoteControl: RemoteControl) {
        Log.d(TAG, "onConnected")
        _connectionState.value = ConnectionState.Connected
        // Request full server status after connecting
        remoteControl.getServerStatus()
    }

    override fun onConnecting(remoteControl: RemoteControl) {
        Log.d(TAG, "onConnecting")
        _connectionState.value = ConnectionState.Connecting
    }

    override fun onDisconnected(remoteControl: RemoteControl, e: Exception?) {
        Log.d(TAG, "onDisconnected")
        _connectionState.value = ConnectionState.Disconnected
        _serverStatus.value = null
    }

    override fun onUpdate(server: ServerStatus) {
        Log.d(TAG, "onUpdate ServerStatus")
        _serverStatus.value = server
    }

    override fun onUpdate(streamId: String, stream: Stream) {
        Log.d(TAG, "onUpdate Stream: $streamId")
        _serverStatus.update { it?.apply { updateStream(stream) } }
    }

    override fun onUpdate(group: Group) {
        Log.d(TAG, "onUpdate Group: ${group.id}")
        _serverStatus.update { status ->
            status?.apply { updateGroup(group) }
        }
    }

    override fun onUpdate(client: Client) {
        Log.d(TAG, "onUpdate Client: ${client.id}")
        _serverStatus.update { status ->
            status?.apply { updateClient(client) }
        }
    }

    override fun onConnect(client: Client) {
        Log.d(TAG, "onConnect: ${client.id}")
        _serverStatus.update { status ->
            status?.apply { updateClient(client) }
        }
    }

    override fun onDisconnect(clientId: String) {
        Log.d(TAG, "onDisconnect: $clientId")
        _serverStatus.update { status ->
            status?.apply {
                val client = getClient(clientId)
                if (client != null) {
                    client.isConnected = false
                    updateClient(client)
                }
            }
        }
    }

    override fun onVolumeChanged(event: RemoteControl.RPCEvent, clientId: String, volume: Volume) {
        Log.d(TAG, "onVolumeChanged: $clientId -> ${volume.percent}% muted=${volume.isMuted}")
        _serverStatus.update { status ->
            status?.apply {
                val client = getClient(clientId)
                if (client != null) {
                    client.setVolume(volume)
                    updateClient(client)
                }
            }
        }
    }

    override fun onLatencyChanged(event: RemoteControl.RPCEvent, clientId: String, latency: Long) {
        // Not needed for player-only UI
    }

    override fun onNameChanged(event: RemoteControl.RPCEvent, clientId: String, name: String) {
        // Not needed for player-only UI
    }

    override fun onMute(event: RemoteControl.RPCEvent, groupId: String, mute: Boolean) {
        Log.d(TAG, "onMute: $groupId muted=$mute")
        // Group mute affects all clients in the group; the server status update
        // will carry the individual client volume changes.
    }

    override fun onStreamChanged(event: RemoteControl.RPCEvent, groupId: String, streamId: String) {
        Log.d(TAG, "onStreamChanged: $groupId -> $streamId")
    }

    override fun onBatchStart() {
        // Not needed for player-only UI
    }

    override fun onBatchEnd() {
        // Not needed for player-only UI
    }

    // --- SnapclientService.SnapclientListener --------------------------------

    override fun onPlayerStart(service: SnapclientService) {
        Log.d(TAG, "onPlayerStart")
        _playerRunning.value = true
        _playerError.value = null
    }

    override fun onPlayerStop(service: SnapclientService) {
        Log.d(TAG, "onPlayerStop")
        _playerRunning.value = false
    }

    override fun onLog(
        service: SnapclientService,
        timestamp: String,
        logClass: String,
        tag: String,
        msg: String,
    ) {
        // Not exposed to UI; could be wired to a debug log flow later.
    }

    override fun onError(service: SnapclientService, msg: String, exception: Exception?) {
        Log.e(TAG, "onPlayerError: $msg", exception)
        _playerError.value = msg
    }

    // --- Local client resolution ---------------------------------------------

    private fun combineLocalClient(): StateFlow<Client?> {
        val result = MutableStateFlow<Client?>(null)
        scope.launch {
            serverStatus.collect { status ->
                result.value = status?.let { resolveLocalClient(it) }
            }
        }
        return result.asStateFlow()
    }

    /**
     * Resolve the local Snapclient by matching the server client whose ID equals
     * the persistent host ID passed to the native client. No fallback to another
     * client.
     */
    fun resolveLocalClient(status: ServerStatus): Client? {
        return status.getClient(localHostId)
    }

    // --- Cleanup -------------------------------------------------------------

    fun cleanup() {
        disconnect()
        unbindService()
        scope.cancel()
    }

    companion object {
        private const val TAG = "SnapcastRepo"
    }
}

/** Connection state of the JSON-RPC control link. */
sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
}
