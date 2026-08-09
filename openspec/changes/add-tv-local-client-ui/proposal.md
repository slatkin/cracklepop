## Why

The current Android TV experience exposes the full Snapserver management hierarchy first, making D-pad navigation unpredictable and obscuring the app's primary TV use case: controlling playback by the Snapclient running on the TV itself.

## What Changes

- Add a TV-only local-client home screen as the default UI on Android TV devices.
- Identify the local Snapclient using the host ID already persisted and passed to the native client.
- Present server connection state, local Snapclient start/stop, local client volume and mute, and the current group and stream.
- Provide deterministic D-pad navigation, an initial focus target, and visible focus treatment across local-client controls.
- Keep the existing server management UI available through a secondary action and preserve the existing default UI on non-TV devices.
- Keep music-source transport, browsing, and metadata outside the app's scope.

## Capabilities

### New Capabilities
- `tv-local-client-control`: TV-first control of the Snapclient running on the local device, including stable remote navigation and access to server management.

### Modified Capabilities

None.

## Impact

- Affects `MainActivity`, its layouts/resources, local Snapclient state presentation, and the handoff to the existing group list.
- Reuses the existing Snapclient service, persisted host ID, Snapserver control connection, and server-management callbacks.
- Does not add dependencies or change Snapcast control protocol behavior.
- Non-TV behavior remains compatible with the current application.
