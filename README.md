# NTFY Notifier Driver for Hubitat

[![CI](https://github.com/graftechnology/hubitat-ntfy-notification-driver/actions/workflows/ci.yml/badge.svg)](https://github.com/graftechnology/hubitat-ntfy-notification-driver/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Hubitat Package Manager](https://img.shields.io/badge/Hubitat-Package%20Manager-blue)](https://hubitatpackagemanager.hubitatcommunity.com/)

A Hubitat Elevation driver that sends notifications through [ntfy](https://ntfy.sh), either the public
ntfy.sh service or your own server. It implements Hubitat's standard Notification capability, so it works
anywhere a notification device can be selected: Rule Machine, Hubitat Safety Monitor, Notifications, custom apps.

## Table of Contents

- [Features](#features)
- [Installation](#installation)
- [Configuration](#configuration)
- [Usage](#usage)
- [Feature Reference](#feature-reference)
- [Troubleshooting](#troubleshooting)
- [Development](#development)
- [Support](#support)
- [License](#license)

## Features

- Works with ntfy.sh or any self-hosted ntfy server over HTTP or HTTPS
- Access token or username/password authentication
- Title, priority (Min to Max), tags with automatic emoji conversion
- Click actions: open a URL or trigger an HTTP request from the notification
- Attachments from a URL, custom notification icons, Markdown formatting
- Scheduling: delay by minutes or deliver at a specific date and time
- Built-in connection test with `connectionStatus`, `lastNotificationStatus` and `lastNotificationTime` attributes
- Clear, actionable error messages and credential-safe debug logging
- Optional support for self-signed certificates

## Installation

### Hubitat Package Manager (recommended)

1. Open **Apps** > **Hubitat Package Manager**.
2. Choose **Install** > **Search by Keywords** and search for `NTFY`.
3. Select **NTFY Notifier Driver** and follow the prompts.

HPM will also offer future updates.

### Manual

1. Open **Drivers Code** and click **New Driver**.
2. Click **Import**, paste the raw URL below, and click **Import** again, then **Save**.

```
https://raw.githubusercontent.com/graftechnology/hubitat-ntfy-notification-driver/main/drivers/graftechnology/ntfy-notifier.groovy
```

Because the driver declares an `importUrl`, you can update later from the same page with the **Import** button.

## Configuration

1. Open **Devices** > **Add Device** > **Virtual**.
2. Give it a name, choose **NTFY Notifier Driver** as the type, and click **Save Device**.
3. Fill in the preferences below and click **Save Preferences**.
4. Click the **Test Connection** command and check your ntfy client.

### Choose a private topic

On the public ntfy.sh server, a topic is effectively a password: anyone who subscribes to the same
topic name receives every message published to it. Use a long, unique, hard-to-guess name such as
`hubitat-a8f3k2p9q7z1`, never something generic like `hubitat` or `home`. Self-hosted servers with
access control do not have this problem.

### Preferences

| Setting                       | Required | Description                                                                                       |
| ----------------------------- | -------- | ------------------------------------------------------------------------------------------------- |
| Protocol                      | Yes      | `https` (recommended) or `http`                                                                   |
| NTFY Host                     | Yes      | Hostname, with port if needed: `ntfy.sh`, `ntfy.example.com:8080`. No protocol, no path.          |
| Topic                         | Yes      | Letters, numbers, `_` and `-` only, up to 64 characters. See the note above.                      |
| Title                         | No       | Notification title. Defaults to `Hubitat`.                                                        |
| Priority                      | No       | Min, Low, Default, High or Max. Defaults to Default.                                              |
| Tags                          | No       | Comma-separated. Names that match an emoji short code appear as emojis.                           |
| Click Action / Action URL     | No       | `Open URL` opens the URL when tapped. `HTTP Request` adds a button that POSTs to the URL.         |
| Attachment URL / Filename     | No       | Attach an image or file from a URL, optionally with a display filename.                           |
| Custom Icon URL               | No       | Replaces the notification icon.                                                                   |
| Enable Markdown Formatting    | No       | Renders `**bold**`, `_italic_`, links and lists in the message body.                              |
| Scheduling / Delay / Time     | No       | `Delay by Minutes` (1 to 4320) or `Send at Specific Time` (`YYYY-MM-DD HH:MM:SS`, hub time zone). |
| Access Token                  | No       | ntfy access token (`tk_...`). Recommended for ntfy Pro and self-hosted servers with auth.         |
| Username / Password           | No       | Basic authentication. Ignored when an access token is set.                                        |
| Ignore SSL certificate errors | No       | Enable only for self-hosted servers with self-signed certificates.                                |
| Enable Debug Logging          | No       | Detailed request logging. Turns itself off after 30 minutes.                                      |

### Example: public ntfy.sh

```
Protocol:  https
NTFY Host: ntfy.sh
Topic:     hubitat-a8f3k2p9q7z1
```

### Example: self-hosted with authentication

```
Protocol:     https
NTFY Host:    ntfy.example.com
Topic:        hubitat
Access Token: tk_xxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

## Usage

### Rule Machine

1. Add an action: **Notifications** > **Send, Speak or Notify a Message**.
2. Choose **Send notification to** and pick your NTFY device.
3. Enter the message, for example `Front door opened at %time%`.

### Hubitat Safety Monitor and other apps

Any app that sends notifications lists the device under its notification device selector. Nothing
else is needed.

### From a custom app or driver

```groovy
device.deviceNotification("Motion detected in the living room")
```

### Test Connection

The **Test Connection** command on the device page publishes a message tagged with a check mark and test
tube, ignoring any scheduling you have configured so the result arrives immediately. The
`connectionStatus` attribute shows the outcome and the hub log explains any failure.

## Feature Reference

### Tags and emojis

Tags are sent as-is. ntfy renders tags that match an emoji short code as emojis in front of the title;
the rest appear as plain text below the message. Example: `warning,house` shows a warning sign and a house.
The full list is in the [ntfy emoji reference](https://docs.ntfy.sh/emojis/).

### Click actions

- **Open URL** sets the notification's click target, for example your Hubitat dashboard.
- **HTTP Request** adds an "Open" button that sends a POST to the URL when tapped. Useful for
  Maker API endpoints or webhooks.

### Attachments

Provide a direct URL to an image or file. The ntfy client shows images inline. Combine with a camera
snapshot URL for security alerts.

### Scheduling

- **Delay by Minutes** holds the message on the server for the given number of minutes (up to 3 days).
- **Send at Specific Time** delivers at the given date and time, interpreted in your hub's time zone.
  The time must be in the future and within 3 days, otherwise the notification fails with a clear error.

Scheduling applies to every notification the device sends, so it suits dedicated devices such as a
"morning digest" notifier rather than general alerts.

### Markdown

With Markdown enabled, messages such as `**CRITICAL**: freezer at *-2°C*. [Dashboard](https://hub.local)`
render with bold, italics and links in clients that support it.

### Authentication

ntfy supports two schemes and this driver sends whichever you configure:

- **Access token**: `Authorization: Bearer tk_...`. Create one with `ntfy token add` on your server or in
  your ntfy.sh account settings. Preferred, because tokens can be revoked individually.
- **Username and password**: standard Basic authentication.

If both are set, the access token wins.

## Troubleshooting

| Log message                                                | What to do                                                                              |
| ---------------------------------------------------------- | --------------------------------------------------------------------------------------- |
| `Configuration error: ...`                                 | The message lists every problem. Fix the named preferences and save.                    |
| `Unknown host`                                             | Check the NTFY Host spelling and that the hub can resolve DNS.                          |
| `Cannot connect to server`                                 | Check the port, firewall rules and that the server is running.                          |
| `Unauthorized (HTTP 401)`                                  | The access token or username/password is wrong or expired.                              |
| `Forbidden (HTTP 403)`                                     | The credentials are valid but cannot publish to this topic. Check the server ACLs.      |
| `Not found (HTTP 404)`                                     | The host serves something other than ntfy at that path, or the topic name is invalid.   |
| `Rate limited (HTTP 429)`                                  | ntfy.sh limits free publishing. Slow down or use a self-hosted server.                  |
| `SSL error`                                                | For self-signed certificates, enable **Ignore SSL certificate errors**.                 |
| `Schedule time ... must be in the future`                  | Update the schedule time or switch scheduling off.                                      |
| Notifications appear that you did not send                 | Your topic is shared with someone else. Pick a unique topic (see above).                |

Enable **Debug Logging** to see the exact URL, headers and body of each request in the hub log. The
`Authorization` header is redacted.

## Development

The driver is tested off-hub with [hubitat_ci](https://github.com/biocomp/hubitat_ci), which loads the
real driver file into a Hubitat-like sandbox, validates its metadata the way the hub does, and lets the
tests mock hub APIs such as `httpPost` and `sendEvent`.

```
./gradlew test
```

Requirements: a JDK 17 or newer on your `PATH` or in `JAVA_HOME` to run Gradle. The JDK 11 toolchain the
tests need is downloaded automatically. Test reports land in `build/reports/tests/test/index.html`.

Layout:

```
drivers/graftechnology/ntfy-notifier.groovy   the driver (the only file installed on the hub)
src/test/groovy/                              Spock specifications
packageManifest.json                          Hubitat Package Manager manifest
```

### Releasing

1. Bump `VERSION` in the driver and `version`, `dateReleased` and `releaseNotes` in `packageManifest.json`.
2. Add a section to `CHANGELOG.md`.
3. Run the tests, commit, tag `vX.Y.Z` and push. CI checks that the driver and manifest versions agree.
4. Create a GitHub release from the tag. HPM reads the manifest from `main`, so users see the update
   once the commit lands there.

## Support

- Issues and feature requests: [GitHub Issues](https://github.com/graftechnology/hubitat-ntfy-notification-driver/issues)
- ntfy documentation: [docs.ntfy.sh](https://docs.ntfy.sh/)
- Hubitat community: [community.hubitat.com](https://community.hubitat.com/)
- More drivers: [Graf Technology HPM repository](https://github.com/graftechnology/hubitat-hpm-repository)

## License

MIT. See [LICENSE](LICENSE).

Copyright (c) 2025-2026 Graf Technology, LLC.
