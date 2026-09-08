# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and versions follow
[Semantic Versioning](https://semver.org/).

## [1.1.1] - 2026-09-08

### Changed

- A password saved without a username logs a warning and sends without authentication, as 1.0.0 did,
  instead of failing validation. Nothing that worked on 1.0.0 should stop working after upgrading.

## [1.1.0] - 2026-09-08

Existing installs update in place. No preference is renamed or removed, so saved settings carry over.

### Added

- Access token authentication (`Authorization: Bearer`). Set the new "Access Token" preference; it takes
  precedence over username/password. Closes #1.
- "Ignore SSL certificate errors" preference for self-hosted servers with self-signed certificates.
- Validation for topic length (ntfy allows 64 characters), hosts that mistakenly include the protocol,
  priority values, and delays or schedule times beyond ntfy's three-day maximum. A password saved without a
  username logs a warning.
- `importUrl` in the driver definition so manual installs can update from the Drivers Code page.
- Automated test suite (Spock + hubitat_ci) and GitHub Actions CI.

### Changed

- The topic no longer defaults to `hubitat`. On the public ntfy.sh server that topic is shared by everyone
  who kept the default, so users received each other's notifications. Existing devices keep their saved topic,
  and the driver now logs a warning on every send and preference save while `hubitat` is still used on ntfy.sh.
  If that is you, change the topic to something unique.
- Test Connection ignores delay/at scheduling so the test message arrives immediately.
- Saving preferences no longer resets `lastNotificationStatus`, `lastNotificationTime` and `connectionStatus`.
- Debug logging turns itself off 30 minutes after install or after saving preferences, following Hubitat convention.
- Configuration errors now set `connectionStatus` to "Configuration Error" for notifications as well as tests.
- Requests declare `Content-Type: text/plain` explicitly.
- The priority list no longer offers a duplicate "Default" entry. The previous value is still honored if stored.
- Tag placeholder text no longer suggests `fire` as an example.

### Fixed

- "Send at Specific Time" delivered at the wrong time. ntfy has no parser for `YYYY-MM-DD HH:MM:SS`, so it
  matched only the clock portion and ignored the date. The driver now converts the time (in the hub's time zone)
  to a Unix timestamp before sending, and refuses times in the past.
- Debug logging printed the `Authorization` header, exposing credentials in the hub log. It is now redacted.
- SSL handshake failures are reported with a hint instead of as an unexpected error.

## [1.0.0] - 2025-06-16

### Added

- Initial release: Notification capability, connection test, titles, priorities, tags, click actions,
  attachments, custom icons, markdown, scheduling and basic authentication.

[1.1.1]: https://github.com/graftechnology/hubitat-ntfy-notification-driver/compare/v1.1.0...v1.1.1
[1.1.0]: https://github.com/graftechnology/hubitat-ntfy-notification-driver/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/graftechnology/hubitat-ntfy-notification-driver/releases/tag/v1.0.0
