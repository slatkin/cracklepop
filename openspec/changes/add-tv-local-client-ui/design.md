## Context

`MainActivity` currently hosts a toolbar, the always-present `GroupListFragment`, and a hidden connection button. It also owns both relevant state sources: the Snapserver `RemoteControl` callbacks and the bound `SnapclientService` callbacks. The group list is adapter-driven; rebinding a group removes and recreates its client controls, so it cannot provide stable focus during server updates.

The native Snapclient already receives a persistent UUID from `SnapclientService.getUniqueId()` as its `--hostID`. Snapserver clients expose that ID, and groups reference their clients and selected stream. Client volume and mute are client-scoped, while stream selection is group-scoped.

See `proposal.md` for motivation and `specs/tv-local-client-control/spec.md` for observable behavior.

## Goals / Non-Goals

**Goals:**
- Add the TV experience without replacing the existing activity, control connection, service, or management components.
- Keep the local control view hierarchy stable while values change so D-pad focus is retained.
- Derive all displayed state from existing settings, service state, and server status rather than introducing a second source of truth.
- Preserve the existing non-TV UI path.

**Non-Goals:**
- Redesigning the existing group/client management UI for TV.
- Changing group membership or group stream from the local-client home.
- Adding source transport, media metadata, media-session handling, or a new playback protocol.
- Introducing an Android TV UI framework or another dependency.

## Decisions

### Use two stable panels within the existing activity

Add a static local-client panel alongside the existing management fragment and switch their visibility according to TV mode and the selected screen. TV devices start on the local panel; non-TV devices leave the management panel visible exactly as today. On TV, the manage-server action reveals the existing fragment and back returns to the local panel.

This keeps all existing callbacks and dialogs attached to `MainActivity`, avoids a new activity or navigation dependency, and makes rollback straightforward. A separate TV activity was considered, but it would duplicate service/control lifecycle wiring and make handoff to existing management more complex.

### Resolve the local client from the native client host ID

For each render, locate the server client whose ID equals `SnapclientService.getUniqueId(context)`, retaining its containing group and stream for display. Do not fall back to the first connected client, client name, network address, or list position. If no exact match exists, render stopped, connecting, or unavailable state from service and connection state without binding controls to another client.

The exact ID is already the protocol identity and remains stable across process restarts. Names and addresses are user-editable or environment-dependent and are therefore unsuitable.

### Render a small state model from existing sources

Centralize local-panel rendering around these externally meaningful states:

```text
UNCONFIGURED
    │ configure/discover
    ▼
SERVER_UNAVAILABLE ── retry/settings
    │ connected
    ▼
CLIENT_STOPPED ── start ──▶ CLIENT_CONNECTING
    ▲                            │ matching client appears
    │ stop/error                 ▼
    └──────────────────── CLIENT_RUNNING
```

The renderer reads server configuration, remote-control connectivity, bound service/running state, and the latest matching server client. Existing lifecycle and RPC callbacks request a render after updating their current state. The renderer updates text, enabled state, visibility, progress, and icons in place; it does not reinflate the local panel.

This is preferable to storing an independent UI state because callbacks can arrive in different orders and duplicated state would become stale.

### Keep home-screen operations client-scoped

Start and stop continue to control the local native Snapclient. Volume and mute use the existing per-client Snapserver operations on the exact matched local client. Group and stream are informational on the home screen because changing stream is group-scoped and could alter playback for other clients. Full group and stream changes remain in server management.

### Use a vertical focus rail with direct slider adjustment

Available actions participate in an explicit up/down order headed by the primary state action, followed by mute, volume, manage server, and settings as applicable. Left/right adjust volume only while the volume control has focus. Select activates buttons, and back follows panel/activity navigation.

The initial focus goes to the primary action for the current state. Rendering preserves focus when the focused view remains available; if it becomes unavailable, focus moves to the new primary action or nearest valid action. State-list resources provide a consistent, high-contrast focused appearance without changing resting visuals.

A geometry-only focus scheme was rejected because the current nested layout already produces unpredictable jumps. An enter-to-adjust slider mode was also rejected as unnecessary complexity for a single linear control.

### Keep management refresh behavior isolated

Server updates may continue rebuilding rows in the existing management fragment. The static local panel is not a child of that adapter and therefore keeps its view identity and focus while its displayed values are refreshed. No broad adapter refactor is required for this change.

## Risks / Trade-offs

- **[Service and control callbacks can arrive out of order]** → Derive each render from current service, connection, and server-status values instead of assuming callback order.
- **[The service may be running before its client appears in server status]** → Show the explicit connecting state and retain a stop action until an exact client match appears.
- **[An external server change can move the local client between groups]** → Resolve the containing group and stream from the latest server status on every render.
- **[Focus can become invalid when state-specific actions disappear]** → Preserve focus only for still-visible controls and otherwise request focus on the current primary action.
- **[The legacy management UI remains imperfect with a remote]** → Keep it secondary and unchanged in this focused change; local-client tasks remain fully remote-operable.
- **[Displaying but not changing stream is less convenient]** → Avoid implying a client-local operation where Snapcast only provides a group-wide change; users can make that change through management.

## Migration Plan

1. Add TV-local resources and panel behavior behind runtime television-mode detection.
2. Retain the existing management path as the default for all non-TV devices.
3. Verify TV lifecycle and focus behavior across each state before release.
4. Roll back by removing the TV panel selection; the existing management fragment, service, and control paths remain intact.
