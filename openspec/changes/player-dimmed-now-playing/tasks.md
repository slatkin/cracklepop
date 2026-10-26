## 1. State Model

- [x] 1.1 Add a `dimmed: Boolean` field to the running state in `PlayerUiState.kt` (default false).
- [x] 1.2 Add a single tunable `dimTimeout` constant (e.g. in the ViewModel companion) used by the inactivity countdown.

## 2. ViewModel Idle Timer

- [x] 2.1 Add an `onUserInput()` entry point on the ViewModel that resets the inactivity countdown and clears `dimmed`.
- [x] 2.2 Add a ViewModel coroutine that runs the countdown only while the client is in the running state and sets `dimmed = true` on timeout; cancel/restart on input and on leaving the running state.

## 3. Player Screen Dimmed Presentation

- [x] 3.1 Attach a root-level `onPreviewKeyEvent`/input hook to the `Box` in `PlayerScreen.kt` that forwards any key or click to the ViewModel's `onUserInput()`.
- [x] 3.2 In `RunningContent`, when `dimmed` is true, render only the stream and group names on a heavily dimmed background (e.g. `EverforestColors.bg0`) and omit the volume, mute, and stop controls; when false, render the existing full controls unchanged.

## 4. Verification

- [x] 4.1 Build the app and run focused Android lint with the documented Java 17 / Android SDK setup.
- [ ] 4.2 On a TV or TV emulator, verify: no input while running for the timeout → dims; any key while dimmed → restores identical controls; input while running resets the timer; stopping the client while dimmed → returns to the stopped state.
- [ ] 4.3 Verify no dimming occurs in unconfigured, server-unavailable, stopped, or connecting states.