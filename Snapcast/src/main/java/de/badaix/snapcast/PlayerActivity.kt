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

package de.badaix.snapcast

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.badaix.snapcast.ui.PlayerViewModel
import de.badaix.snapcast.ui.PlayerUiState
import de.badaix.snapcast.ui.PlayerScreen
import de.badaix.snapcast.ui.ServerSetupScreen
import de.badaix.snapcast.ui.theme.EverforestDarkScheme
import de.badaix.snapcast.ui.theme.EverforestTypography
import androidx.tv.material3.MaterialTheme

/**
 * Compose host activity — the single LEANBACK_LAUNCHER entry point.
 * Replaces the former MainActivity (touch-based management UI).
 */
class PlayerActivity : ComponentActivity() {

    private val viewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Bind to the SnapclientService
        viewModel.bindService(this)

        // Try auto-reconnect to persisted server
        viewModel.retryConnection()

        setContent {
            MaterialTheme(
                colorScheme = EverforestDarkScheme,
                typography = EverforestTypography,
            ) {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                CracklePopApp(
                    uiState = uiState,
                    onSetupServer = { viewModel.setupServer() },
                    onRetryConnection = { viewModel.retryConnection() },
                    onStartListening = { viewModel.startListening() },
                    onStopListening = { viewModel.stopListening() },
                    onVolumeChanged = { viewModel.setVolume(it) },
                    onToggleMute = { viewModel.toggleMute() },
                    onServerSelected = { host, port -> viewModel.onServerSelected(host, port) },
                )
            }
        }
    }

    override fun onDestroy() {
        viewModel.unbindService(this)
        super.onDestroy()
    }
}

@Composable
fun CracklePopApp(
    uiState: PlayerUiState,
    onSetupServer: () -> Unit,
    onRetryConnection: () -> Unit,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onVolumeChanged: (Int) -> Unit,
    onToggleMute: () -> Unit,
    onServerSelected: (String, Int) -> Unit,
) {
    var showServerSetup by remember { mutableStateOf(false) }

    when {
        showServerSetup -> {
            ServerSetupScreen(
                onServerSelected = { host: String, port: Int ->
                    onServerSelected(host, port)
                    showServerSetup = false
                },
                onBack = { showServerSetup = false },
            )
        }
        else -> {
            PlayerScreen(
                uiState = uiState,
                onSetupServer = { showServerSetup = true },
                onRetryConnection = onRetryConnection,
                onStartListening = onStartListening,
                onStopListening = onStopListening,
                onVolumeChanged = onVolumeChanged,
                onToggleMute = onToggleMute,
            )
        }
    }
}
