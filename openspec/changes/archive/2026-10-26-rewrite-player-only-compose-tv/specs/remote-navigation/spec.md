## Purpose

Guarantee a predictable, legible 10-foot experience driven entirely by a D-pad remote: known initial focus, deterministic directional movement, focus that survives live state updates, and a focus indicator visible across the room.

## ADDED Requirements

### Requirement: Deterministic initial focus

Each screen SHALL place focus on a defined initial target when it becomes active, appropriate to the current state.

#### Scenario: Entering the player screen

- **WHEN** the player screen becomes active in a given state
- **THEN** focus is placed on that state's primary action (for example, set up a server, retry, start, or stop)

### Requirement: Predictable directional navigation

Focusable controls SHALL have deterministic up/down/left/right relationships so that every D-pad press moves focus to a defined, reachable control with no dead ends.

#### Scenario: Traversing controls with the D-pad

- **WHEN** the user presses a direction on the remote
- **THEN** focus moves to the defined neighbouring control in that direction, or remains in place when there is no neighbour

#### Scenario: Adjusting volume with left/right

- **WHEN** the volume control has focus and the user presses left or right
- **THEN** the volume decreases or increases and focus stays on the volume control

### Requirement: Focus retention across state updates

The application SHALL preserve focus on the currently focused control while displayed values refresh. When a state change removes the focused control, focus SHALL move to the current primary action or nearest valid control.

#### Scenario: Value refresh keeps focus

- **WHEN** server status or player state updates while a still-available control is focused
- **THEN** focus remains on that control

#### Scenario: Focused control disappears

- **WHEN** a state change hides the focused control
- **THEN** focus moves to the new primary action or nearest valid control rather than being lost

### Requirement: Visible focus treatment

The application SHALL render a high-contrast focus indicator on the focused control that is legible at typical TV viewing distance, without altering the resting appearance of unfocused controls.

#### Scenario: Focused control is distinguishable

- **WHEN** any control holds focus
- **THEN** it is visually distinct from unfocused controls at 10-foot distance
