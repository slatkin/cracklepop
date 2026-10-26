# player-dimmed-now-playing Specification

## Purpose

Provides the TV player screen's in-app dimmed "now playing" presentation: after inactivity while the client is running, the full control surface fades to a heavily dimmed read-only view of the current stream and group, and any key press restores the controls.

## Requirements

### Requirement: Dim after inactivity while running

The application SHALL enter a dimmed now-playing state when the local client is running and no user input has been received for a configurable inactivity timeout. The dimmed state SHALL NOT activate in any non-running player state (unconfigured, server-unavailable, client-stopped, client-connecting) nor while the client is paused or not receiving audio.

#### Scenario: Running with no input for the timeout

- **WHEN** the local client is running and no key press or click has been received for the configured inactivity timeout
- **THEN** the screen transitions to the dimmed now-playing state

#### Scenario: No dimming outside the running state

- **WHEN** the player is in any state other than running (unconfigured, server-unavailable, stopped, connecting)
- **THEN** the screen never transitions to the dimmed state

#### Scenario: Client stops while dimmed

- **WHEN** playback stops or the client leaves the running state while the dimmed state is active
- **THEN** the screen returns to the normal rendering for the current player state without requiring user input

### Requirement: Dimmed rendering

The dimmed state SHALL suppress the running-state control surface (volume, mute, stop) and render only minimal read-only information about the current playback on a heavily dimmed background. The application SHALL show at least the current stream name and group name in the dimmed state.

#### Scenario: Dimmed state shows minimal playback info

- **WHEN** the screen is in the dimmed state
- **THEN** only the current stream and group names are shown on a heavily dimmed background, and the volume, mute, and stop controls are not rendered

#### Scenario: No track metadata available

- **WHEN** the snapcast server provides no track/album metadata for the current stream
- **THEN** the dimmed state still shows the stream and group names (the data the application already has) and omits any unavailable metadata

### Requirement: Wake from dimmed on any user input

The application SHALL exit the dimmed state and restore the full running-state control surface on any key press or click while dimmed. The inactivity timer SHALL reset on every input while in the running state so that steady interaction never triggers the dim.

#### Scenario: Any key wakes the screen

- **WHEN** the screen is in the dimmed state and the user presses any key or clicks any control
- **THEN** the screen returns to the full running-state controls with the same volume, mute, and group/stream information as before dimming

#### Scenario: Active input resets the timer

- **WHEN** the user provides input during the running state before the timeout elapses
- **THEN** the inactivity timer resets and the dimmed state is not entered

### Requirement: Timeout is configurable

The inactivity timeout SHALL be defined by a single tunable constant in the application so the dim timing can be adjusted without behavioral or structural changes.

#### Scenario: Timeout value is defined in one place

- **WHEN** the application builds the player screen
- **THEN** the inactivity timeout is read from a single constant that can be changed without altering any other behavior
