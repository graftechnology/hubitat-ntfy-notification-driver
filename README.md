# NTFY Notifier Driver for Hubitat

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Hubitat Package Manager](https://img.shields.io/badge/Hubitat-Package%20Manager-blue)](https://hubitatpackagemanager.hubitatcommunity.com/)

A comprehensive Hubitat driver that sends notifications via [NTFY](https://ntfy.sh) with full feature support including attachments, scheduling, markdown formatting, and interactive click actions.

## Table of Contents

- [NTFY Notifier Driver for Hubitat](#ntfy-notifier-driver-for-hubitat)
  - [Table of Contents](#table-of-contents)
  - [Features](#features)
    - [Core Functionality](#core-functionality)
    - [Advanced NTFY Features](#advanced-ntfy-features)
    - [Developer Features](#developer-features)
  - [Installation](#installation)
    - [Hubitat Package Manager (HPM)](#hubitat-package-manager-hpm)
    - [Manual Installation](#manual-installation)
  - [Configuration](#configuration)
    - [Basic Settings](#basic-settings)
    - [Advanced Features](#advanced-features)
    - [Public ntfy.sh Server](#public-ntfysh-server)
    - [Self-Hosted NTFY Server](#self-hosted-ntfy-server)
  - [Usage Examples](#usage-examples)
    - [Basic Notifications](#basic-notifications)
    - [Rich Notifications with Features](#rich-notifications-with-features)
    - [Rule Machine Integration](#rule-machine-integration)
    - [Testing the Connection](#testing-the-connection)
  - [Feature Documentation](#feature-documentation)
    - [Tags and Emojis](#tags-and-emojis)
    - [Click Actions](#click-actions)
    - [Attachments](#attachments)
    - [Message Scheduling](#message-scheduling)
    - [Markdown Formatting](#markdown-formatting)
    - [Custom Icons](#custom-icons)
  - [Troubleshooting](#troubleshooting)
    - [Using the Connection Test](#using-the-connection-test)
    - [Common Issues](#common-issues)
    - [Debug Mode](#debug-mode)
  - [Support](#support)
  - [License](#license)

## Features

### Core Functionality

- ✅ **Universal Compatibility**: Works with `ntfy.sh` or self-hosted servers
- ✅ **Protocol Selection**: HTTP/HTTPS support
- ✅ **Authentication**: Username/password for private servers
- ✅ **Connection Testing**: Built-in test functionality
- ✅ **State Management**: Real-time status tracking
- ✅ **Error Handling**: Comprehensive error management with clear messages

### Advanced NTFY Features

- ✅ **Tags & Emojis**: Text tags with automatic emoji conversion
- ✅ **Click Actions**: Interactive notifications (open URLs, trigger HTTP requests)
- ✅ **Attachments**: Send images/files from URLs
- ✅ **Message Scheduling**: Delay or schedule notifications for specific times
- ✅ **Markdown Formatting**: Rich text with bold, italic, links, lists
- ✅ **Custom Icons**: Override notification icons with custom URLs
- ✅ **Priority Levels**: 5 priority levels (Min, Low, Default, High, Max)
- ✅ **Custom Titles**: Personalized notification titles

### Developer Features

- ✅ **Input Validation**: Prevents configuration errors
- ✅ **Debug Logging**: Detailed troubleshooting information
- ✅ **Professional Error Messages**: Clear, actionable error descriptions
- ✅ **Hubitat Standards**: Native Notification capability implementation

## Installation

### Hubitat Package Manager (HPM)

**Recommended method** for easy installation and automatic updates.

1. Open your Hubitat Elevation® web interface
2. Navigate to **Apps** → **Hubitat Package Manager**
3. Click **Install** → **Search by Keywords**
4. Search for "NTFY Notifier Driver"
5. Select the driver and click **Next**
6. Review the details and click **Install**

### Manual Installation

1. Navigate to **Drivers Code** in your Hubitat interface
2. Click **New Driver**
3. Copy and paste the driver code from [`drivers/graftechnology/ntfy-notifier.groovy`](drivers/graftechnology/ntfy-notifier.groovy)
4. Click **Save**

## Configuration

### Basic Settings

1. Go to **Devices** → **Add Device** → **Virtual**
2. Fill in device details:
   - **Device Name**: "NTFY Notifier"
   - **Device Label**: Your preferred label
   - **Type**: "NTFY Notifier Driver"
3. Click **Save Device**

### Advanced Features

Configure these optional settings in **Device Preferences**:

| Setting                 | Description                 | Example                                   |
| ----------------------- | --------------------------- | ----------------------------------------- |
| **Protocol**            | HTTP or HTTPS               | `https` (recommended)                     |
| **NTFY Host**           | Server hostname             | `ntfy.sh` or `your-server.com`            |
| **Topic**               | Notification topic          | `hubitat-alerts`                          |
| **Title**               | Default message title       | `Hubitat Notification`                    |
| **Priority**            | Message priority (1-5)      | `3` (Default)                             |
| **Tags**                | Comma-separated tags        | `warning,house,fire`                      |
| **Click Action**        | What happens when clicked   | `Open URL` or `HTTP Request`              |
| **Action URL**          | URL for click actions       | `https://your-hubitat.local`              |
| **Attachment URL**      | Image/file to attach        | `http://camera.local/snapshot.jpg`        |
| **Attachment Filename** | Custom filename             | `camera-snapshot.jpg`                     |
| **Custom Icon URL**     | Override notification icon  | `https://your-site.com/icon.png`          |
| **Markdown**            | Enable rich text formatting | `true` or `false`                         |
| **Scheduling**          | When to send message        | `Send Immediately`, `Delay`, or `At Time` |
| **Username/Password**   | For private servers         | Your credentials                          |

### Public ntfy.sh Server

```
Protocol: https
NTFY Host: ntfy.sh
Topic: your-unique-topic-name
Username/Password: (leave blank)
```

### Self-Hosted NTFY Server

```
Protocol: https (or http)
NTFY Host: your-ntfy-server.com
Topic: your-topic
Username/Password: (if authentication enabled)
```

## Usage Examples

### Basic Notifications

Send a simple notification from any Hubitat app:

```groovy
// In Rule Machine or custom app
def ntfyDevice = getDevice("NTFY Notifier")
ntfyDevice.deviceNotification("Motion detected in living room!")
```

### Rich Notifications with Features

Configure your device with these settings for enhanced notifications:

- **Tags**: `warning,house`
- **Click Action**: `Open URL`
- **Action URL**: `https://your-hubitat.local/device/edit/123`
- **Markdown**: `true`

Then send:

```groovy
ntfyDevice.deviceNotification("**ALERT**: Motion detected in *living room* at ${new Date()}")
```

This creates a notification with:

- Warning and house emojis
- Bold "ALERT" text and italic "living room"
- Clickable to open your device page

### Rule Machine Integration

1. Create a new **Rule Machine** rule
2. Set your trigger (e.g., contact sensor opens)
3. Add action: **Send or speak a message**
4. Select your NTFY device
5. Enter your message with markdown: `**Door Alert**: Front door opened at %time%`

### Testing the Connection

1. Open your NTFY device page
2. Click **Test Connection** command
3. Check your NTFY client for the test message
4. Verify **Current States** shows "Connected"

## Feature Documentation

### Tags and Emojis

Tags automatically convert to emojis based on NTFY's built-in mapping:

```
Tags: warning,house,fire
Result: ⚠️🏠🔥 (emojis appear in notification)
```

Popular tags: `warning`, `fire`, `rotating_light`, `house`, `car`, `money`, `computer`, `phone`, `bell`

### Click Actions

Make notifications interactive:

**Open URL**: Opens a webpage when notification is clicked

```
Click Action: Open URL
Action URL: https://your-hubitat.local/device/edit/123
```

**HTTP Request**: Triggers an HTTP call when clicked

```
Click Action: HTTP Request
Action URL: https://your-server.com/api/action
```

### Attachments

Send images or files with notifications:

```
Attachment URL: http://192.168.1.100/camera/snapshot.jpg
Attachment Filename: front-door-camera.jpg
```

Perfect for security camera snapshots, charts, or log files.

### Message Scheduling

**Delay Messages**: Send after a specific time

```
Scheduling: Delay by Minutes
Delay Minutes: 30
```

**Schedule for Specific Time**: Send at exact date/time

```
Scheduling: Send at Specific Time
Schedule Time: 2025-06-17 08:00:00
```

### Markdown Formatting

Enable rich text formatting:

```
Markdown: true
Message: "**CRITICAL**: System temperature is *85°C*. [View Dashboard](https://hub.local)"
```

Supports: **bold**, _italic_, [links](url), lists, code blocks, etc.

### Custom Icons

Override the default notification icon:

```
Custom Icon URL: https://your-site.com/hubitat-icon.png
```

## Troubleshooting

### Using the Connection Test

1. Configure your device settings
2. Click **Test Connection** command
3. Check logs for detailed results
4. Verify test message appears in your NTFY client

### Common Issues

| Issue                       | Solution                                            |
| --------------------------- | --------------------------------------------------- |
| **"Unknown host"**          | Check NTFY Host spelling and network connectivity   |
| **"Unauthorized"**          | Verify username/password for private servers        |
| **"Forbidden"**             | Topic may be protected - try a different topic      |
| **"Connection timeout"**    | Check firewall settings and server availability     |
| **"Invalid topic"**         | Use only letters, numbers, underscores, and hyphens |
| **"Action URL required"**   | Provide Action URL when using click actions         |
| **"Invalid schedule time"** | Use format: YYYY-MM-DD HH:MM:SS                     |

### Debug Mode

Enable debug logging in device preferences for detailed troubleshooting information including:

- HTTP request/response details
- Header information
- Validation results
- Error stack traces

## Support

- **Documentation**: [NTFY Official Docs](https://docs.ntfy.sh/)
- **Community**: [Hubitat Community Forum](https://community.hubitat.com/)
- **Issues**: [GitHub Issues](https://github.com/graftechnology/hubitat-ntfy-notification-driver/issues)
- **Other Drivers**: [Graf Technology HPM Repository](https://github.com/graftechnology/hubitat-hpm-repository)

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

**Graf Technology, LLC** - Professional Hubitat integrations and automation solutions.
