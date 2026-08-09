## Purpose

Provide Android TV users with a remote-friendly home screen for controlling the Snapcast client running on the TV while retaining access to full server management.

## ADDED Requirements

### Requirement: TV devices default to local-client control
The application SHALL present the local-client control screen as the default screen when running in television UI mode. The application SHALL preserve the existing server-management screen as the default on non-TV devices.

#### Scenario: Launch on a TV device
- **WHEN** the application launches in television UI mode
- **THEN** the local-client control screen is presented before the server-management hierarchy

#### Scenario: Launch on a non-TV device
- **WHEN** the application launches outside television UI mode
- **THEN** the existing server-management screen remains the default

### Requirement: Local Snapclient identity
The TV UI SHALL associate local controls with the Snapserver client whose host ID matches the identity used by the Snapclient running on the device. It MUST NOT select another server client as the local client when no match exists.

#### Scenario: Local client is present
- **WHEN** server status contains a client whose host ID matches the local Snapclient identity
- **THEN** the TV UI presents that client's status and controls

#### Scenario: Local client is not yet present
- **WHEN** no server client matches the local Snapclient identity
- **THEN** the TV UI presents an unavailable or connecting local-client state without exposing another client's controls

### Requirement: Connection and Snapclient lifecycle states
The TV UI SHALL present a clear state and a relevant primary action for an unconfigured server, an unavailable server, a stopped local Snapclient, a connecting local Snapclient, and a running local Snapclient.

#### Scenario: No server is configured
- **WHEN** the application has no Snapserver configuration
- **THEN** the TV UI offers an action to configure or discover a server

#### Scenario: Configured server is unavailable
- **WHEN** the configured Snapserver control connection cannot be established
- **THEN** the TV UI reports that the server is unavailable and offers retry or server settings

#### Scenario: Local Snapclient is stopped
- **WHEN** a Snapserver is configured and the local Snapclient is not running
- **THEN** the TV UI offers an action to start the local Snapclient

#### Scenario: Local Snapclient is connecting
- **WHEN** the local Snapclient has been started but is not yet represented as an available local client
- **THEN** the TV UI reports a connecting state and allows the user to stop the local Snapclient

#### Scenario: Local Snapclient is running
- **WHEN** the local Snapclient is running and its server client is available
- **THEN** the TV UI reports that it is receiving audio and offers an action to stop it

### Requirement: Local Snapcast controls
When the local server client is available, the TV UI SHALL allow the user to change that client's volume and mute state through existing Snapcast control operations. The TV UI SHALL display the local client's current group and stream without presenting music-source transport controls.

#### Scenario: Change local volume
- **WHEN** the user changes volume from the local-client screen
- **THEN** only the matched local client's volume is updated through Snapcast control

#### Scenario: Toggle local mute
- **WHEN** the user activates the mute control
- **THEN** only the matched local client's mute state is toggled through Snapcast control

#### Scenario: Display current assignment
- **WHEN** the matched local client belongs to a group with an assigned stream
- **THEN** the TV UI displays the current group and stream

#### Scenario: No music transport controls
- **WHEN** the local-client screen is displayed
- **THEN** it does not offer source play, pause, previous, next, browsing, metadata, or artwork controls

### Requirement: Predictable remote navigation
The local-client screen SHALL be fully operable with D-pad up, down, left, right, select, and back. Every focused control MUST have a clearly visible focus state, and the screen SHALL place focus on a relevant actionable control when first shown.

#### Scenario: Navigate the local-client controls
- **WHEN** the user repeatedly presses D-pad up or down
- **THEN** focus follows a deterministic order among the currently available controls

#### Scenario: Adjust focused volume
- **WHEN** the volume control has focus and the user presses D-pad left or right
- **THEN** the local client's volume decreases or increases respectively

#### Scenario: Server status changes
- **WHEN** server events refresh values while a local-client control remains available
- **THEN** focus remains on that control rather than moving unpredictably

### Requirement: Secondary server management
The local-client screen SHALL provide an action that opens the existing server-management UI. Back navigation from server management SHALL return to the local-client screen on TV devices.

#### Scenario: Open server management
- **WHEN** the user activates the manage-server action
- **THEN** the existing group and client management UI is displayed

#### Scenario: Return from server management
- **WHEN** the user presses back from server management on a TV device
- **THEN** the local-client screen is restored with a relevant control focused
