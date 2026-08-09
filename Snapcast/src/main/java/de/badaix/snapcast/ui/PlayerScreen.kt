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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import de.badaix.snapcast.ui.theme.EverforestColors

/**
 * Main player screen. Renders each [PlayerUiState] with tv-material3 controls:
 * primary state action, stop, mute, volume, and read-only group/stream.
 *
 * Focus: deterministic initial focus per state on the primary action;
 * directional navigation with no dead ends; volume left/right adjustment;
 * high-contrast focus treatment.
 */
@Composable
fun PlayerScreen(
    uiState: PlayerUiState,
    onSetupServer: () -> Unit,
    onRetryConnection: () -> Unit,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onVolumeChanged: (Int) -> Unit,
    onToggleMute: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when (uiState) {
                is PlayerUiState.Unconfigured -> UnconfiguredContent(
                    onSetupServer = onSetupServer,
                )
                is PlayerUiState.ServerUnavailable -> ServerUnavailableContent(
                    host = uiState.configuredHost,
                    onRetry = onRetryConnection,
                    onChangeServer = onSetupServer,
                )
                is PlayerUiState.ClientStopped -> StoppedContent(
                    groupName = uiState.groupName,
                    streamName = uiState.streamName,
                    onStart = onStartListening,
                )
                is PlayerUiState.ClientConnecting -> ConnectingContent(
                    onStop = onStopListening,
                )
                is PlayerUiState.ClientRunning -> RunningContent(
                    volume = uiState.volume,
                    muted = uiState.muted,
                    groupName = uiState.groupName,
                    streamName = uiState.streamName,
                    onStop = onStopListening,
                    onVolumeChanged = onVolumeChanged,
                    onToggleMute = onToggleMute,
                )
            }
        }
    }
}

// --- Unconfigured state ------------------------------------------------------

@Composable
private fun UnconfiguredContent(
    onSetupServer: () -> Unit,
) {
    val primaryFocus = remember { FocusRequester() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "No Snapserver configured",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Discover a server on your network or enter one manually.",
            style = MaterialTheme.typography.bodyLarge,
            color = EverforestColors.grey0,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(48.dp))
        FocusedButton(
            text = "Configure server",
            focusRequester = primaryFocus,
            onClick = onSetupServer,
            colors = ButtonDefaults.colors(
                containerColor = EverforestColors.green,
                contentColor = EverforestColors.bg0,
            ),
        )
    }
}

// --- Server unavailable state ------------------------------------------------

@Composable
private fun ServerUnavailableContent(
    host: String,
    onRetry: () -> Unit,
    onChangeServer: () -> Unit,
) {
    val primaryFocus = remember { FocusRequester() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Snapserver unavailable",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.error,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "$host",
            style = MaterialTheme.typography.bodyMedium,
            color = EverforestColors.grey0,
        )
        Spacer(modifier = Modifier.height(48.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            FocusedButton(
                text = "Retry connection",
                focusRequester = primaryFocus,
                onClick = onRetry,
                colors = ButtonDefaults.colors(
                    containerColor = EverforestColors.green,
                    contentColor = EverforestColors.bg0,
                ),
            )
            FocusedButton(
                text = "Change server",
                onClick = onChangeServer,
            )
        }
    }
}

// --- Client stopped state ----------------------------------------------------

@Composable
private fun StoppedContent(
    groupName: String?,
    streamName: String?,
    onStart: () -> Unit,
) {
    val primaryFocus = remember { FocusRequester() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Ready to listen",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(16.dp))
        InfoRow(groupName = groupName, streamName = streamName)
        Spacer(modifier = Modifier.height(48.dp))
        FocusedButton(
            text = "Start listening",
            focusRequester = primaryFocus,
            onClick = onStart,
            colors = ButtonDefaults.colors(
                containerColor = EverforestColors.green,
                contentColor = EverforestColors.bg0,
            ),
        )
    }
}

// --- Client connecting state -------------------------------------------------

@Composable
private fun ConnectingContent(
    onStop: () -> Unit,
) {
    val primaryFocus = remember { FocusRequester() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Connecting this device…",
            style = MaterialTheme.typography.headlineLarge,
            color = EverforestColors.yellow,
        )
        Spacer(modifier = Modifier.height(48.dp))
        FocusedButton(
            text = "Stop",
            focusRequester = primaryFocus,
            onClick = onStop,
            colors = ButtonDefaults.colors(
                containerColor = EverforestColors.red,
                contentColor = EverforestColors.bg0,
            ),
        )
    }
}

// --- Client running state ----------------------------------------------------

@Composable
private fun RunningContent(
    volume: Int,
    muted: Boolean,
    groupName: String?,
    streamName: String?,
    onStop: () -> Unit,
    onVolumeChanged: (Int) -> Unit,
    onToggleMute: () -> Unit,
) {
    val stopFocus = remember { FocusRequester() }
    val muteFocus = remember { FocusRequester() }
    val volumeFocus = remember { FocusRequester() }

    var currentVolume by remember { mutableStateOf(volume) }

    // Sync volume when it changes from the server
    if (currentVolume != volume) {
        currentVolume = volume
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Receiving audio",
            style = MaterialTheme.typography.headlineLarge,
            color = EverforestColors.green,
        )
        Spacer(modifier = Modifier.height(16.dp))
        InfoRow(groupName = groupName, streamName = streamName)
        Spacer(modifier = Modifier.height(48.dp))

        // Volume control with left/right adjustment
        VolumeControl(
            volume = currentVolume,
            muted = muted,
            focusRequester = volumeFocus,
            onVolumeChanged = { newVol ->
                currentVolume = newVol
                onVolumeChanged(newVol)
            },
        )
        Spacer(modifier = Modifier.height(32.dp))

        // Stop and Mute buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            FocusedButton(
                text = "Stop",
                focusRequester = stopFocus,
                onClick = onStop,
                colors = ButtonDefaults.colors(
                    containerColor = EverforestColors.red,
                    contentColor = EverforestColors.bg0,
                ),
            )
            FocusedButton(
                text = if (muted) "Unmute" else "Mute",
                focusRequester = muteFocus,
                onClick = onToggleMute,
                colors = ButtonDefaults.colors(
                    containerColor = if (muted) EverforestColors.yellow else EverforestColors.bg3,
                    contentColor = if (muted) EverforestColors.bg0 else EverforestColors.fg,
                ),
            )
        }
    }
}

// --- Shared composables ------------------------------------------------------

@Composable
private fun InfoRow(
    groupName: String?,
    streamName: String?,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        groupName?.let {
            InfoLabel(label = "Group", value = it)
        }
        streamName?.let {
            InfoLabel(label = "Stream", value = it)
        }
        if (groupName == null && streamName == null) {
            Text(
                text = "Unassigned",
                style = MaterialTheme.typography.bodyMedium,
                color = EverforestColors.grey0,
            )
        }
    }
}

@Composable
private fun InfoLabel(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = EverforestColors.grey0,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

/**
 * Volume control with left/right D-pad adjustment.
 * When focused, left decreases and right increases volume.
 */
@Composable
private fun VolumeControl(
    volume: Int,
    muted: Boolean,
    focusRequester: FocusRequester,
    onVolumeChanged: (Int) -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }

    Card(
        onClick = {},
        modifier = Modifier
            .focusRequester(focusRequester)
            .onFocusChanged { isFocused = it.isFocused || it.hasFocus },
        colors = CardDefaults.colors(
            containerColor = if (isFocused) EverforestColors.bg4 else EverforestColors.bg3,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Decrease button
            Card(
                onClick = {
                    val newVol = (volume - 5).coerceIn(0, 100)
                    onVolumeChanged(newVol)
                },
                modifier = Modifier.size(48.dp),
                colors = CardDefaults.colors(
                    containerColor = EverforestColors.bg5,
                ),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "−",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }

            // Volume display
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (muted) "Muted" else "$volume%",
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (muted) EverforestColors.yellow else MaterialTheme.colorScheme.onBackground,
                )
                // Volume bar
                Box(
                    modifier = Modifier
                        .width(200.dp)
                        .height(8.dp)
                        .background(EverforestColors.bg5),
                ) {
                    Box(
                        modifier = Modifier
                            .width((200 * volume / 100).dp)
                            .height(8.dp)
                            .background(
                                if (muted) EverforestColors.yellow else EverforestColors.green,
                            ),
                    )
                }
            }

            // Increase button
            Card(
                onClick = {
                    val newVol = (volume + 5).coerceIn(0, 100)
                    onVolumeChanged(newVol)
                },
                modifier = Modifier.size(48.dp),
                colors = CardDefaults.colors(
                    containerColor = EverforestColors.bg5,
                ),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "+",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }
    }
}

/**
 * Button with deterministic focus handling.
 * When [focusRequester] is provided, it requests focus on composition.
 * High-contrast focus treatment via tv-material3's built-in scale/border.
 */
@Composable
private fun FocusedButton(
    text: String,
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null,
    colors: androidx.tv.material3.ButtonColors? = null,
) {
    var isFocused by remember { mutableStateOf(false) }

    val button = @Composable {
        Button(
            onClick = onClick,
            modifier = Modifier
                .then(
                    if (focusRequester != null) {
                        Modifier
                            .focusRequester(focusRequester)
                            .onFocusChanged { isFocused = it.isFocused || it.hasFocus }
                    } else {
                        Modifier.onFocusChanged { isFocused = it.isFocused || it.hasFocus }
                    },
                ),
            colors = colors ?: ButtonDefaults.colors(),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }

    if (focusRequester != null) {
        androidx.compose.runtime.LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }

    button()
}
