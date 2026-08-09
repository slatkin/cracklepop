## Purpose

Provide TV-first control of the Snapclient running on this device: connection state, start/stop, this client's volume and mute, and a read-only view of its current group and stream — all rendered from a single, deterministic state model.

## ADDED Requirements

### Requirement: Resolve the local client by exact host ID

The application SHALL identify the local Snapclient by matching the server client whose ID equals the persistent host ID passed to the native client (`SnapclientService.getUniqueId`). The application SHALL NOT fall back to another client by name, network address, list position, or first-connected heuristic.

#### Scenario: Matching client present in server status

- **WHEN** server status contains a client whose ID equals the local host ID
- **THEN** that client is treated as the local client and its volume, mute, containing group, and stream are used for display and control

#### Scenario: No matching client in server status

- **WHEN** the server is connected but no client ID matches the local host ID
- **THEN** the application renders a stopped or connecting state and binds no controls to any other client

### Requirement: Single player state model

The application SHALL render the player screen from one state model derived from server configuration, control-connection status, bound-service state, and the latest matching server client. The model SHALL express these externally observable states: unconfigured, server-unavailable, client-stopped, client-connecting, and client-running.

#### Scenario: No server configured

- **WHEN** no server has ever been configured or discovered
- **THEN** the screen presents the unconfigured state with a primary action to set up a server

#### Scenario: Server configured but not reachable

- **WHEN** a server is configured but the control connection is not established
- **THEN** the screen presents the server-unavailable state with a retry action

#### Scenario: Connected and idle

- **WHEN** the control connection is established and the local Snapclient is not running
- **THEN** the screen presents the client-stopped state with a start action

#### Scenario: Started but not yet visible on server

- **WHEN** the local Snapclient has been started but no matching client has appeared in server status yet
- **THEN** the screen presents the client-connecting state and retains a stop action

#### Scenario: Running and controllable

- **WHEN** the local Snapclient is running and a matching client is present in server status
- **THEN** the screen presents the client-running state with stop, volume, and mute controls and the current group and stream

#### Scenario: Callbacks arriving out of order

- **WHEN** service, connection, and server-status updates arrive in any order
- **THEN** the rendered state reflects the current combined values rather than the order of arrival

### Requirement: Start and stop the local Snapclient

The application SHALL start and stop only the native Snapclient running on this device, using the existing foreground service.

#### Scenario: Start from stopped

- **WHEN** the user activates start in the client-stopped state
- **THEN** the native Snapclient service starts and the screen transitions to client-connecting until a matching client appears

#### Scenario: Stop while running or connecting

- **WHEN** the user activates stop in the client-running or client-connecting state
- **THEN** the native Snapclient service stops and the screen transitions to client-stopped

### Requirement: Control local client volume and mute

The application SHALL adjust volume and mute for only the exactly matched local client using the existing per-client Snapcast control operations. Updates that only refresh the displayed value SHALL NOT emit control changes back to the server.

#### Scenario: User changes volume

- **WHEN** the user adjusts the volume control in the client-running state
- **THEN** a per-client volume change is sent for the matched local client only

#### Scenario: User toggles mute

- **WHEN** the user activates mute in the client-running state
- **THEN** the matched local client's mute is toggled and reflected on screen

#### Scenario: External volume change refreshes display

- **WHEN** the server reports a volume change for the matched local client
- **THEN** the displayed volume updates without sending a control change back to the server

### Requirement: Display current group and stream read-only

The application SHALL display the matched local client's current group and current stream as informational values. The application SHALL NOT provide stream reassignment or group-membership changes from the player screen.

#### Scenario: Running client shows its assignment

- **WHEN** the local client is running and assigned to a group with a selected stream
- **THEN** the group and stream names are shown as read-only text

#### Scenario: Server moves the client between groups

- **WHEN** an external change reassigns the local client to a different group or stream
- **THEN** the displayed group and stream update on the next render from current server status
