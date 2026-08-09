## Why

CracklePop inherited snapdroid's touch-first design: a full Snapserver *management console* (every group, every client on the LAN) with a bundled native player as a secondary concern. On a TV with a remote, the management hierarchy is the wrong primary surface, and the code that drives it — a 1045-line `MainActivity` implementing four listener interfaces with no single source of truth — makes the 10-foot experience (deterministic focus, reliable state) hard to get right and harder to change.

The valuable, hard-won parts of this app are the Snapcast JSON-RPC control layer and the bundled native `libsnapclient.so` player. Both are already UI-free and cleanly isolated. This change discards the touch-era UI shell and rebuilds a minimal, remote-first player on top of the existing backend.

## What Changes

- **BREAKING** Remove the Snapserver management UI entirely: `MainActivity`, `GroupListFragment`, `GroupItem`, `ClientItem`, `ClientSettingsActivity/Fragment`, `GroupSettingsActivity/Fragment`, `ServerDialogFragment`, `AboutActivity`, and all associated touch XML layouts/menus. The app no longer manages other clients, groups, group membership, stream assignment, client names, or latency. Multi-room management moves to other tools (Snapweb, other clients).
- Rebuild the UI as a single-screen, remote-first player in **Kotlin + Jetpack Compose for TV** (`androidx.tv:tv-material3`), driven by one `ViewModel` exposing a single `StateFlow` of player state.
- Keep the existing Java backend as-is and call it from Kotlin: `control/` (RemoteControl, TcpClient, JsonRPC, `json/*` models), `SnapclientService`, and `utils/` (NsdHelper, Settings, MD5). Add a thin Kotlin adapter that turns the backend's listener callbacks into a `Flow`.
- Present exactly the player-scoped operations: connection state, start/stop the local Snapclient, this device's volume and mute (resolved by exact `hostID` match), and read-only display of the local client's current group and stream.
- Provide server setup as a minimal secondary flow: mDNS auto-discovery with a manual host/port fallback, persisted via the existing `Settings`.
- Make deterministic D-pad focus, an initial focus target, and high-contrast focus treatment first-class requirements across every screen.
- Adopt the Everforest dark-hard palette as Compose theme tokens (carrying forward the intent of the in-progress theme change).
- Supersede and abandon the in-progress `add-tv-local-client-ui` change, which bolts a panel onto the `MainActivity` this rewrite deletes.

## Capabilities

### New Capabilities
- `local-player-control`: TV-first control of the Snapclient running on this device — connection state, start/stop, this client's volume/mute resolved by exact host ID, and read-only current group/stream — rendered from a single state model.
- `server-discovery-setup`: Discover Snapservers via mDNS, allow manual host/port entry as a fallback, and persist the selected server for auto-reconnect.
- `remote-navigation`: Deterministic D-pad navigation and 10-foot focus behavior across all screens — initial focus, predictable directional movement, focus retention across state updates, and visible high-contrast focus.

### Modified Capabilities

None. (No specs exist in `openspec/specs/` yet; the removed management UI was never spec-tracked and is captured as a BREAKING removal above.)

## Impact

- **Removed:** ~12 Java UI files and their XML layouts/menus (the entire management surface).
- **Kept unchanged:** the native player (`libsnapclient.so`, CMake/NDK submodule), the `control/` JSON-RPC layer, `SnapclientService`, and `utils/` discovery/settings — all already UI-free.
- **Added:** Kotlin + Compose toolchain (Kotlin Gradle plugin, Compose BOM, `androidx.tv:tv-material3`, lifecycle-viewmodel-compose, coroutines). minSdk 21 is retained (Compose supports it); the native build path is untouched.
- **Build/manifest:** the app declares a single launcher activity (Compose host) with `LEANBACK_LAUNCHER`; the removed activities/receivers-for-management drop out of the manifest. The `BroadcastReceiver` and foreground service remain.
- **Licensing:** unchanged — still a GPL-3.0-or-later fork of snapdroid; source-availability obligations carry over.
- **Non-goals:** no source transport/browsing/metadata, no media-session integration, no change to the Snapcast control protocol, no native player changes, no multi-room management.
