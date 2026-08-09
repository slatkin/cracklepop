## Purpose

Let a TV user connect the app to a Snapserver with minimal effort: discover servers automatically over the local network, fall back to manual entry, and remember the choice so the app reconnects on its own next time.

## ADDED Requirements

### Requirement: Discover servers via mDNS

The application SHALL discover Snapservers on the local network using mDNS service discovery and present resolved servers for selection.

#### Scenario: A server is discovered

- **WHEN** the app is in the unconfigured or server-unavailable state and a Snapserver is discovered on the network
- **THEN** the discovered server (host and control port) is offered as a selectable option

#### Scenario: No server discovered

- **WHEN** discovery runs and no server is found within a reasonable time
- **THEN** the app presents the manual-entry fallback without blocking the user

### Requirement: Manual host and port entry

The application SHALL allow the user to enter a server host and ports manually as a fallback, with the control port defaulting from the stream port when not specified.

#### Scenario: User enters a server manually

- **WHEN** the user submits a host and stream port
- **THEN** the app uses that host, stream port, and a control port (explicit, or derived from the stream port) to connect

### Requirement: Persist selection and auto-reconnect

The application SHALL persist the selected server and SHALL attempt to reconnect to it automatically on next launch without requiring re-entry.

#### Scenario: Reconnect on launch

- **WHEN** the app launches and a server was previously selected
- **THEN** the app attempts to connect to the persisted server without prompting for setup

#### Scenario: Change the configured server

- **WHEN** the user selects or enters a different server
- **THEN** the new server replaces the persisted selection and becomes the reconnect target
