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

import android.net.nsd.NsdServiceInfo
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
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
 * Server setup screen: mDNS discovery with manual host/port fallback.
 * Presents resolved servers for selection; persists the choice via Settings.
 */
@Composable
fun ServerSetupScreen(
    onServerSelected: (host: String, streamPort: Int) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val discoveredServers = remember { mutableStateListOf<DiscoveredServer>() }
    var showManualEntry by remember { mutableStateOf(false) }

    // mDNS discovery
    DisposableEffect(Unit) {
        val nsdHelper = de.badaix.snapcast.utils.NsdHelper.getInstance(context)
        val listener = object : de.badaix.snapcast.utils.NsdHelper.NsdHelperListener {
            override fun onResolved(nsdHelper: de.badaix.snapcast.utils.NsdHelper, serviceInfo: NsdServiceInfo) {
                val host = serviceInfo.host?.hostAddress ?: serviceInfo.host?.hostName ?: return
                val port = serviceInfo.port
                val server = DiscoveredServer(
                    name = serviceInfo.serviceName,
                    host = host,
                    streamPort = port,
                )
                // Avoid duplicates
                if (discoveredServers.none { it.host == server.host && it.streamPort == server.streamPort }) {
                    discoveredServers.add(server)
                }
            }
        }

        nsdHelper.startListening("_snapcast._tcp.", "", listener)

        onDispose {
            nsdHelper.stopListening()
        }
    }

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
        ) {
            Text(
                text = "Configure Server",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(32.dp))

            if (showManualEntry) {
                ManualEntryForm(
                    onServerSelected = onServerSelected,
                    onCancel = { showManualEntry = false },
                )
            } else {
                if (discoveredServers.isEmpty()) {
                    Text(
                        text = "Searching for Snapservers on your network…",
                        style = MaterialTheme.typography.bodyLarge,
                        color = EverforestColors.grey0,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "If no server appears, you can enter one manually.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = EverforestColors.grey0,
                        textAlign = TextAlign.Center,
                    )
                } else {
                    Text(
                        text = "Discovered Servers",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    discoveredServers.forEach { server ->
                        ServerCard(
                            server = server,
                            onSelect = { onServerSelected(server.host, server.streamPort) },
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    val manualFocus = remember { FocusRequester() }
                    FocusedButton(
                        text = "Enter manually",
                        focusRequester = manualFocus,
                        onClick = { showManualEntry = true },
                        colors = ButtonDefaults.colors(
                            containerColor = EverforestColors.blue,
                            contentColor = EverforestColors.bg0,
                        ),
                    )
                    FocusedButton(
                        text = "Back",
                        onClick = onBack,
                    )
                }
            }
        }
    }
}

/**
 * Manual host/port entry form for TV (D-pad navigable).
 */
@Composable
private fun ManualEntryForm(
    onServerSelected: (String, Int) -> Unit,
    onCancel: () -> Unit,
) {
    var host by remember { mutableStateOf("") }
    var streamPort by remember { mutableStateOf("1704") }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Manual Entry",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Host: $host",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Stream Port: $streamPort (default: 1704)",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(8.dp))
        val controlPort = streamPort.toIntOrNull()?.plus(1) ?: 1705
        Text(
            text = "Control Port: $controlPort (auto-derived)",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            val connectFocus = remember { FocusRequester() }
            FocusedButton(
                text = "Connect",
                focusRequester = connectFocus,
                onClick = {
                    val port = streamPort.toIntOrNull() ?: 1704
                    onServerSelected(host.ifEmpty { "localhost" }, port)
                },
                colors = ButtonDefaults.colors(
                    containerColor = EverforestColors.green,
                    contentColor = EverforestColors.bg0,
                ),
            )
            FocusedButton(
                text = "Cancel",
                onClick = onCancel,
            )
        }
    }
}

@Composable
private fun ServerCard(
    server: DiscoveredServer,
    onSelect: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }

    Card(
        onClick = onSelect,
        modifier = Modifier
            .fillMaxWidth(0.6f)
            .onFocusChanged { isFocused = it.isFocused || it.hasFocus },
        colors = CardDefaults.colors(
            containerColor = if (isFocused) EverforestColors.bg4 else EverforestColors.bg2,
        ),
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = server.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${server.host}:${server.streamPort}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EverforestColors.grey0,
                )
            }
        }
    }
}

data class DiscoveredServer(
    val name: String,
    val host: String,
    val streamPort: Int,
)

/**
 * Button with deterministic focus handling.
 */
@Composable
private fun FocusedButton(
    text: String,
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null,
    colors: androidx.tv.material3.ButtonColors? = null,
) {
    var isFocused by androidx.compose.runtime.remember { mutableStateOf(false) }

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
