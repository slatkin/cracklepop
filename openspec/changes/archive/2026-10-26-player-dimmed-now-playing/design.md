## Context

See `proposal.md` — Why. Current-state facts relevant to this design:

- The player screen is a single Compose-for-TV screen, `PlayerScreen.kt`, that renders a sealed `PlayerUiState` (`Unconfigured`, `ServerUnavailable`, `ClientStopped`, `ClientConnecting`, `ClientRunning`) via a `when` switch. The running state renders volume/mute/stop controls plus a read-only group/stream row (`RunningContent`).
- The screen has no key handling today: focus moves among `tv-material3` controls and D-pad left/right adjusts volume. There is no global input hook, no idle timer, and no `keepScreenOn` flag anywhere in the app (verified: no `FLAG_KEEP_SCREEN_ON` usage).
- State lives in `PlayerViewModel.kt` as a single `StateFlow<PlayerUiState>`; `PlayerUiState.kt` holds volume, muted, group/stream names. The UI is a pure function of that state.
- Android TV offers no API for an app to dim or power off the panel, and a system Daydream would not know what a snapcast client is playing. The dimmed state is therefore pure in-app Compose presentation.

## Goals / Non-Goals

**Goals:**

- Dim the running state to a minimal now-playing view after a tunable inactivity timeout, waking on any input — implemented entirely in the Compose layer.
- Keep the existing single-source-of-truth model: dimming is presentation derived from player state plus an input-idle signal, not a new backend state.

**Non-Goals:**

- No system API usage, permissions, `DreamService`, Daydream registration, or `FLAG_KEEP_SCREEN_ON` changes (that flag would prevent dimming, the opposite of this feature).
- No new settings UI or persistence for the timeout — a single constant suffices per spec.
- No changes to the Java backend, native player, or manifest.

## Decisions

### Drive the dim from an idle signal in the ViewModel, not a UI timer

Expose the dim as part of the state model: add a `dimmed: Boolean` to the running state (or a wrapper state) in `PlayerUiState.kt`, and have `PlayerViewModel` own a single coroutine that (a) runs an inactivity countdown while the client is running, (b) sets `dimmed = true` on timeout, (c) resets to `false` on any input event. The screen remains a pure function of `PlayerUiState`.

*Why:* the existing design's core discipline is "render is derived, never imperatively patched." A timer inside a `@Composable` would fight recomposition and state changes (stop-while-dimmed, server-driven state swaps) and would be hard to test. Putting the countdown in the ViewModel keeps it testable without an emulator and makes "client stopped while dimmed" fall out naturally: when the state stops being running, the dimmed flag is simply not rendered.

Alternative considered — a pure-UI idle timer in `PlayerScreen` with `onPreviewKeyEvent` resetting it — was rejected because it couples timing to composition and leaves the stop-while-dimmed case to be handled by UI code.

### Reset the timer on input at the root, not per-control

Attach one `Modifier.onPreviewKeyEvent` (and/or a pointer/click hook) at the root `Box` of `PlayerScreen` that forwards "any key" to the ViewModel as `onUserInput()`. Each event cancels/restarts the countdown. No per-control wiring.

*Why:* the spec requires *any* key to both reset the timer (while running) and wake from dimmed. A single root-level hook covers both uniformly and avoids touching every control. `onPreviewKeyEvent` is chosen because it sees events before focus consumers, so even a key that a focused control consumes still counts as user input.

### Model the dim as a presentation overlay inside the running state

Keep one `PlayerUiState` type. Add `dimmed: Boolean` to the running state; when `true`, `RunningContent` renders the minimal dimmed layout (stream + group names on a heavily dimmed scrim, e.g. the `EverforestColors.bg0` background) instead of the control surface. The wake path is `dimmed = false` on input, which restores the identical controls.

*Why:* mirroring how the existing screen branches on state, a boolean in the running state is the smallest change that satisfies "wake restores the same volume/mute/group/stream." A separate `DimmedRunning` state type would duplicate the running-state data for no behavioral gain.

### Inactivity timeout as a single constant

Define the timeout in `PlayerUiState.kt` (or the ViewModel companion) as one `val dimTimeout: Duration` used by the countdown. Per spec it must be a single tunable value.

### Do not set FLAG_KEEP_SCREEN_ON

The dim is an in-app visual only. When the user genuinely walks away, the OS will still enter Ambient Mode / Energy Saver after its own timers and the panel will go off — which is the desired end state. Setting `FLAG_KEEP_SCREEN_ON` would actively prevent that. Explicit non-goal: this change must not add it.

## Risks / Trade-offs

- **Compose key interception subtleties** → `onPreviewKeyEvent` at the root may not fire for some key sources (e.g. media/play keys routed to the media session); acceptable for v1 since D-pad/select/back are the interaction surface on TV. Media keys are out of scope.
- **Idle timer vs. recomposition** → countdown lives in the ViewModel coroutine, so recompositions of the screen don't reset or leak timers; the coroutine is scoped to the ViewModel lifecycle.
- **Dimed state surprise on long listening sessions** → mitigated by wake-on-any-key being immediate and by the timeout being a single tunable constant (also the spec's requirement).
- **No tests currently exist in the repo** → the ViewModel countdown is structured so a pure-JVM unit test can drive `advanceTimeBy` over the timeout; wiring the test task is optional and not a blocker (see tasks).

## Migration Plan

1. Add the dimmed flag and timeout constant to the state model; ViewModel owns the countdown coroutine and `onUserInput()`.
2. Extend `RunningContent` to render the dimmed presentation when `dimmed` is true; add the root input hook to `PlayerScreen`.
3. Verify on a TV/emulator: idle → dims after timeout; any key → wakes with identical controls; stop while dimmed → returns to the stopped state; input while running resets the timer.
4. Rollback: revert the small Compose/ViewModel diff; nothing else in the app changes.

## Open Questions

- Default timeout duration (e.g. 30s vs 60s) — a UX tuning value; the constant makes it a one-line change without touching specs or tasks.