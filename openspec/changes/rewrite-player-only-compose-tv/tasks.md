## 1. Toolchain and Dependencies

- [x] 1.1 Add the Kotlin Gradle plugin and Kotlin stdlib to the module, keeping Java 17 compile options and the existing native build untouched.
- [x] 1.2 Add the Compose BOM, `androidx.tv:tv-material3`, `androidx.activity:activity-compose`, `androidx.lifecycle:lifecycle-viewmodel-compose`, and coroutines; enable `buildFeatures.compose`.
- [x] 1.3 Confirm the app still builds with `JAVA_HOME=<jdk-17> ./gradlew build` before removing any existing UI.

## 2. Backend Flow Adapter

- [x] 2.1 Add a Kotlin repository that owns `RemoteControl` and the bound `SnapclientService` and implements their existing listener interfaces.
- [x] 2.2 Expose connection state, latest `ServerStatus`, and player start/stop/error as `StateFlow`/`SharedFlow` from the repository.
- [x] 2.3 Expose start/stop, per-client volume/mute (by matched client), and server selection as suspend/plain functions delegating to the backend.
- [x] 2.4 Resolve the local client by exact `SnapclientService.getUniqueId` match, with no fallback to another client.

## 3. Player State Model

- [x] 3.1 Define the sealed `PlayerUiState` covering unconfigured, server-unavailable, client-stopped, client-connecting, and client-running (per `specs/local-player-control`).
- [x] 3.2 Add a `ViewModel` that collects the repository flows and derives a single `StateFlow<PlayerUiState>` from combined current values (order-independent).
- [x] 3.3 Route volume refreshes so displayed value updates do not emit control changes back to the server.

## 4. Player Screen (Compose for TV)

- [x] 4.1 Add the Compose host `ComponentActivity` and set it as the `LEANBACK_LAUNCHER` entry point.
- [x] 4.2 Render each `PlayerUiState` with `tv-material3` controls: primary state action, stop, mute, volume, and read-only group/stream.
- [x] 4.3 Wire the primary action, start/stop, volume, and mute to the ViewModel/repository for the matched local client only.

## 5. Server Setup Flow

- [x] 5.1 Add mDNS discovery (existing `NsdHelper`) presenting resolved servers for selection.
- [x] 5.2 Add a manual host/port entry fallback, deriving control port from stream port when unspecified.
- [x] 5.3 Persist the selection via existing `Settings` and auto-reconnect to it on launch.

## 6. Remote Navigation

- [x] 6.1 Set deterministic initial focus per state on the primary action.
- [x] 6.2 Define directional (up/down/left/right) relationships with no dead ends; reserve left/right on the focused volume control for adjustment.
- [x] 6.3 Preserve focus across value refreshes and move it to a valid control when the focused control is hidden by a state change.
- [x] 6.4 Apply a high-contrast focus treatment legible at 10-foot distance without changing resting appearance.

## 7. Theme

- [x] 7.1 Express the Everforest dark-hard palette and 10-foot typography as a Compose `ColorScheme`/typography set.

## 8. Remove Management Shell

- [x] 8.1 Delete `MainActivity`, `GroupListFragment`, `GroupItem`, `ClientItem`, `ClientSettingsActivity/Fragment`, `GroupSettingsActivity/Fragment`, `ServerDialogFragment`, `AboutActivity`, and their touch XML layouts/menus.
- [x] 8.2 Remove the deleted activities from the manifest, retaining the foreground service and `BroadcastReceiver`.
- [x] 8.3 Remove any now-unused management-only backend methods only if they become orphaned; keep the control layer otherwise intact.

## 9. Verification

- [ ] 9.1 Build the app and run focused Android lint with the documented Java 17 / Android SDK setup.
- [ ] 9.2 On a TV or TV emulator, verify D-pad and select behavior across unconfigured, server-unavailable, stopped, connecting, and running states, including focus retention during server events.
- [ ] 9.3 Verify volume and mute affect only the matched local Snapserver client, start/stop drive the native player, and auto-reconnect works on launch.
