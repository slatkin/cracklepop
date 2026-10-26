## Context

See `proposal.md` — Why. The relevant current-state facts:

- The Java backend is already UI-free and separable. The `control/` package imports only `android.text.TextUtils`, `android.util.Log`, and `androidx.annotation.NonNull`; it speaks Snapcast JSON-RPC over a raw socket (`TcpClient`) with a reader/writer thread pair. `SnapclientService` launches `libsnapclient.so` via `ProcessBuilder` with a persistent `--hostID` from `SnapclientService.getUniqueId`, behind a four-method listener interface. `utils/NsdHelper` does mDNS discovery of `_snapcast._tcp.`; `utils/Settings` persists `host`, `streamPort` (1704), `controlPort` (streamPort+1), and `autoStart`.
- The value lives in that backend and in the native player. The cost lives in the UI shell: a 1045-line `MainActivity` implementing four listener interfaces with no single source of truth, plus fragments, adapters, dialogs, and touch XML.

This design keeps the backend and native player and replaces only the shell.

## Goals / Non-Goals

**Goals:**

- Delete the management UI and stand up a single-screen, remote-first player without touching the backend or native player.
- Have exactly one source of truth (a `ViewModel` state) that every backend callback feeds, so render is a pure function of state.
- Reuse the existing Java backend from Kotlin via a thin callback→`Flow` adapter, with no protocol or lifecycle rewrite.

**Non-Goals:**

- Converting `control/`, `SnapclientService`, or `utils/` to Kotlin in this change (revisit later).
- Any change to the JSON-RPC protocol, the native build, or the CMake/NDK submodule.
- A settings/preferences surface beyond what server setup requires.

## Decisions

### Keep the Java backend; wrap it in a Kotlin Flow adapter

Add a Kotlin `SnapcastRepository` (or equivalent) that owns the `RemoteControl` and the bound `SnapclientService`, implements their existing listener interfaces, and republishes their callbacks as `StateFlow`/`SharedFlow` (connection state, latest `ServerStatus`, player start/stop/error). The `ViewModel` collects those flows, resolves the local client by exact host ID, and derives a single `PlayerUiState`.

*Why:* the backend already works and is UI-free; rewriting it invites protocol regressions for no user-visible gain. Kotlin calls Java seamlessly, so a mixed-language module is normal and low-risk. Alternative — port the backend to Kotlin/coroutines now — was rejected as unnecessary scope with real regression risk on the one part that is hard to get right.

### One ViewModel state, rendered by Compose

Model the screen as a sealed `PlayerUiState` covering unconfigured, server-unavailable, client-stopped, client-connecting, and client-running (see `specs/local-player-control`). The `ViewModel` exposes a single `StateFlow<PlayerUiState>`; a top-level `@Composable` `when`-switches on it. Backend callbacks only update backing values and recompute the state; they never touch views.

*Why:* the current design's hardest problems — out-of-order callbacks, manual focus preservation, duplicated UI state — all stem from having no single source of truth. A `StateFlow` + recomposition dissolves them: render is derived, never imperatively patched. This also makes the state machine directly testable without an emulator.

### Compose for TV for the view layer

Build the UI with `androidx.tv:tv-material3` on a single `ComponentActivity` host set as the `LEANBACK_LAUNCHER` entry point. Use TV components' built-in focus handling for deterministic D-pad movement and focus indication; use `Modifier.focusRequester`/`focusRestorer` for the per-state initial focus and retention required by `specs/remote-navigation`.

*Why:* the player is essentially one screen with a handful of controls, so the UI surface is small and Compose-for-TV gives correct 10-foot focus behavior without the manual focus gymnastics the deleted code needed. Alternative — Leanback (`androidx.leanback`) — was rejected as the older, heavier, browse/rows-oriented framework that doesn't fit a single control screen.

### Server setup as a minimal secondary flow

mDNS discovery (existing `NsdHelper`) offers resolved servers; a manual host/port form is the fallback; the selection persists through the existing `Settings` and drives auto-reconnect on launch (see `specs/server-discovery-setup`). This is a small second screen or dialog reached from the unconfigured/unavailable states, not a general preferences area.

*Why:* player-only scope means setup is the only configuration the user needs, and discovery should make even that rare.

### Theme as Compose tokens carrying the Everforest palette

Express the Everforest dark-hard palette as a Compose `ColorScheme`/typography set sized for 10-foot legibility, superseding the in-progress `everforest-dark-hard-default` XML theme work.

*Why:* the rewrite replaces the XML view layer the earlier theme change targeted, so the palette should live as Compose tokens rather than XML styles.

## Risks / Trade-offs

- **Mixed Java/Kotlin module** → normal for Android; the boundary is the repository adapter, keeping Java confined to backend/native concerns.
- **Backend still uses raw threads and a callback bag** → contained behind the repository; the `ViewModel` sees only flows. Converting to coroutines is deferred, not required.
- **Abandoning `add-tv-local-client-ui` (13/15 done)** → its Java panel is discarded, but its state-machine and focus decisions are carried forward into these specs, so the thinking is preserved even though the code is not.
- **Compose/Kotlin toolchain added** → Kotlin Gradle plugin + Compose BOM + `tv-material3`; minSdk 21 is retained (Compose supports 21) and the native build path is untouched, so the added surface is UI-only.
- **Foreground service + notification behavior must survive the shell swap** → the service and `BroadcastReceiver` are kept as-is; only their callers change. Verify start/stop, notification, and boot-autostart against the new host activity.

## Migration Plan

1. Add the Kotlin/Compose toolchain and `tv-material3` dependency; keep the existing app building alongside.
2. Add the Kotlin repository adapter over `RemoteControl` and `SnapclientService`; verify it reports connection, server status, and player lifecycle correctly.
3. Build the Compose player screen and setup flow against the repository, driven by `PlayerUiState`.
4. Switch the manifest launcher to the Compose host activity; remove the management activities, fragments, adapters, dialogs, and touch XML.
5. Verify player lifecycle, volume/mute on the exact local client, focus behavior per state, and auto-reconnect on a TV or TV emulator.
6. Rollback: revert the manifest launcher change; the deleted management shell is recoverable from git history if needed, and the backend is unchanged throughout.

## Open Questions

- Discovery-timeout duration and whether to auto-select a single discovered server vs. always confirm — a UX tuning detail that does not change the specs or task breakdown.
