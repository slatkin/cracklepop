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

import de.badaix.snapcast.control.json.Client
import de.badaix.snapcast.control.json.Group
import de.badaix.snapcast.control.json.Stream
import de.badaix.snapcast.repository.ConnectionState

/**
 * Single state model for the player screen. Render is a pure function of this
 * state; backend callbacks only update backing values and recompute.
 *
 * States: unconfigured, server-unavailable, client-stopped, client-connecting,
 * client-running (per specs/local-player-control).
 */
sealed class PlayerUiState {

    /** No server has ever been configured or discovered. */
    object Unconfigured : PlayerUiState()

    /** A server is configured but the control connection is not established. */
    data class ServerUnavailable(
        val configuredHost: String,
        val configuredPort: Int,
    ) : PlayerUiState()

    /** Connected and idle; the local Snapclient is not running. */
    data class ClientStopped(
        val localClient: Client?,
        val groupName: String?,
        val streamName: String?,
    ) : PlayerUiState()

    /** Started but no matching client has appeared in server status yet. */
    data class ClientConnecting(
        val localClient: Client?,
        val groupName: String?,
        val streamName: String?,
    ) : PlayerUiState()

    /** Running and controllable: volume, mute, read-only group/stream. */
    data class ClientRunning(
        val localClient: Client,
        val volume: Int,
        val muted: Boolean,
        val groupName: String?,
        val streamName: String?,
    ) : PlayerUiState()
}

/**
 * Lightweight snapshot of the server-side [Client] for the UI, avoiding
 * coupling to the full JSON model in Compose rendering.
 */
data class ClientDisplayInfo(
    val id: String,
    val name: String,
    val volume: Int,
    val muted: Boolean,
    val groupId: String?,
    val groupName: String?,
    val streamId: String?,
    val streamName: String?,
)

/**
 * Resolve display info for a matched local client from the current
 * [ServerStatus]. Returns null when the client is not found.
 */
fun resolveClientDisplay(
    serverStatus: de.badaix.snapcast.control.json.ServerStatus?,
    localClientId: String,
): ClientDisplayInfo? {
    if (serverStatus == null) return null
    val client = serverStatus.getClient(localClientId) ?: return null
    // Find the group containing this client
    var containingGroup: Group? = null
    for (group in serverStatus.groups) {
        if (group.getClient(localClientId) != null) {
            containingGroup = group
            break
        }
    }
    val stream = containingGroup?.streamId?.let { serverStatus.getStream(it) }
    return ClientDisplayInfo(
        id = client.id,
        name = client.visibleName,
        volume = client.config.volume.percent,
        muted = client.config.volume.isMuted,
        groupId = containingGroup?.id,
        groupName = containingGroup?.name,
        streamId = stream?.id,
        streamName = stream?.name,
    )
}
