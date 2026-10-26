## Why

While a snapcast client is running on Android TV, the screen stays at full brightness with the full volume/stop/mute control surface visible even when the user is just listening and not interacting. There is no system API to dim the panel, and a system screensaver (Daydream) would not know what this app is playing. Users want the Spotify-style behavior: while listening, the UI fades to a dimmed "now playing" state; any key press brings the full controls back.

## What Changes

- Add an in-app dimmed now-playing state that activates after a period of no user input while the local client is running.
- While dimmed, render only minimal information (stream and group name) on a heavily dimmed scrim; suppress the full control surface.
- Any key press or click wakes the screen back to the full running controls.
- The dim never engages in non-running states (unconfigured, server-unavailable, stopped, connecting) or when the client stops.
- The inactivity timeout is a UX-tunable value; no new system permissions or APIs are required — this is pure Compose overlay behavior on the existing player screen.

## Capabilities

### New Capabilities
- `player-dimmed-now-playing`: The player screen's dimmed now-playing behavior — when it activates, what it renders, and how it wakes.

### Modified Capabilities
<!-- None: existing capabilities describe the running-state controls; this adds an idle presentation layer on top, not a change to control semantics. -->

## Impact

- **Code**: `Snapcast/src/main/java/de/badaix/snapcast/ui/PlayerScreen.kt` (running-state presentation, scrim, wake handling) and `PlayerUiState.kt` / `PlayerViewModel.kt` (dim state flag, timeout ownership).
- **No changes** to the Java backend (`control/`, `SnapclientService`), the native snapcast submodule, the manifest, permissions, or dependencies.
- **Behavior**: running-state screen may self-dim after inactivity; this is externally observable but non-breaking — wake restores the exact previous controls.