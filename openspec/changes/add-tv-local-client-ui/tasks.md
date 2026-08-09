## 1. TV Local-Client Surface

- [x] 1.1 Add the static local-client panel and its status, primary action, mute, volume, assignment, manage-server, and settings views to the main activity layout while retaining the existing management fragment.
- [x] 1.2 Add the required strings and focused-state resources, preserving the existing resting visual theme and providing a high-contrast focus indicator.

## 2. TV Mode and Panel Navigation

- [x] 2.1 Detect television UI mode in `MainActivity`, show the local-client panel by default on TV, and preserve the existing management default on non-TV devices.
- [x] 2.2 Wire manage-server and back behavior to switch between the stable local panel and existing management fragment on TV devices.

## 3. Local Snapclient State

- [x] 3.1 Resolve the local server client by exact match with `SnapclientService.getUniqueId()`, including its current group and stream, without falling back to another client.
- [x] 3.2 Add a single in-place local-panel renderer for unconfigured, server-unavailable, client-stopped, client-connecting, and client-running states.
- [x] 3.3 Invoke the renderer from relevant activity, service-binding, player lifecycle, remote-control connection, and server-status callbacks so asynchronous updates converge on current state.

## 4. Local Snapcast Controls

- [x] 4.1 Wire the state-specific primary action to existing server configuration/discovery, retry, and local Snapclient start/stop behavior.
- [x] 4.2 Wire local mute and volume controls to existing per-client Snapcast operations for only the exactly matched local client, while preventing render-driven volume updates from sending control changes.
- [x] 4.3 Display the matched client's current group and stream as informational values and keep group-wide stream changes in the management UI.

## 5. Remote Focus Behavior

- [x] 5.1 Define deterministic up/down focus relationships for every available local-panel action and reserve left/right on the focused volume control for adjustment.
- [x] 5.2 Request focus on the current primary action when entering the local panel, preserve the focused view across value refreshes, and move focus to a valid action when state changes hide it.

## 6. Verification

- [x] 6.1 Build the app and run focused Android lint with the repository's documented Java 17 and Android SDK setup.
- [ ] 6.2 On an Android TV or TV emulator, manually verify D-pad and select behavior for unconfigured, unavailable, stopped, connecting, and running states, including focus retention during server events.
- [ ] 6.3 Verify local volume and mute affect only the Shield's matched Snapserver client, management opens and returns correctly, and the existing non-TV default UI remains unchanged.
