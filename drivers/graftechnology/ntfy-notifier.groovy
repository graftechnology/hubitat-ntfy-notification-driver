/**
 * NTFY Notifier Driver for Hubitat Elevation
 *
 * Sends Hubitat notifications to an ntfy server (ntfy.sh or self-hosted) with support for
 * titles, priorities, tags, click actions, attachments, icons, markdown, scheduling and auth.
 *
 * Copyright (c) 2025-2026 Graf Technology, LLC
 * Licensed under the MIT License. See LICENSE in the repository root.
 *
 * Source and documentation: https://github.com/graftechnology/hubitat-ntfy-notification-driver
 * Changelog: https://github.com/graftechnology/hubitat-ntfy-notification-driver/blob/main/CHANGELOG.md
 */

import groovy.transform.Field

@Field static final String VERSION = "1.1.1"
@Field static final String TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss"
@Field static final int MAX_TOPIC_LENGTH = 64
@Field static final int MAX_DELAY_MINUTES = 3 * 24 * 60
@Field static final int HTTP_TIMEOUT_SECONDS = 30
@Field static final int DEBUG_LOG_AUTO_OFF_SECONDS = 30 * 60
@Field static final String PUBLIC_HOST = "ntfy.sh"
@Field static final String LEGACY_DEFAULT_TOPIC = "hubitat"

metadata {
    definition(
        name: "NTFY Notifier Driver",
        namespace: "graftechnology",
        author: "Graf Technology, LLC",
        importUrl: "https://raw.githubusercontent.com/graftechnology/hubitat-ntfy-notification-driver/main/drivers/graftechnology/ntfy-notifier.groovy"
    ) {
        capability "Notification"

        command "testConnection"

        attribute "lastNotificationStatus", "string"
        attribute "lastNotificationTime", "string"
        attribute "connectionStatus", "string"
    }

    preferences {
        input name: "ntfyProtocol", type: "enum", title: "Protocol", options: ["https", "http"], defaultValue: "https", required: true
        input name: "ntfyHost", type: "text", title: "NTFY Host (e.g., ntfy.sh or ntfy.example.com:8080)", defaultValue: "ntfy.sh", required: true
        input name: "ntfyTopic", type: "text", title: "Topic (pick a unique, hard-to-guess name: topics on ntfy.sh are public)", required: true
        input name: "ntfyTitle", type: "text", title: "Optional Title", defaultValue: "Hubitat", required: false
        input name: "ntfyPriority", type: "enum", title: "Optional Priority", defaultValue: "3", required: false,
              options: ["1": "Min", "2": "Low", "3": "Default", "4": "High", "5": "Max"]
        input name: "ntfyTags", type: "text", title: "Optional Tags (comma-separated, e.g., warning,house - emoji names auto-convert)", required: false
        input name: "ntfyClickAction", type: "enum", title: "Optional Click Action", defaultValue: "none", required: false,
              options: ["none": "None", "view": "Open URL", "http": "HTTP Request"]
        input name: "ntfyActionUrl", type: "text", title: "Action URL (required if click action selected)", required: false
        input name: "ntfyAttachUrl", type: "text", title: "Optional Attachment URL (image/file to attach)", required: false
        input name: "ntfyAttachFilename", type: "text", title: "Optional Attachment Filename (if URL provided)", required: false
        input name: "ntfyIconUrl", type: "text", title: "Optional Custom Icon URL", required: false
        input name: "ntfyMarkdown", type: "bool", title: "Enable Markdown Formatting", defaultValue: false, required: false
        input name: "ntfyScheduling", type: "enum", title: "Optional Message Scheduling", defaultValue: "none", required: false,
              options: ["none": "Send Immediately", "delay": "Delay by Minutes", "at": "Send at Specific Time"]
        input name: "ntfyDelayMinutes", type: "number", title: "Delay Minutes (1-4320, if delay selected)", required: false
        input name: "ntfyScheduleTime", type: "text", title: "Schedule Time (YYYY-MM-DD HH:MM:SS in the hub's time zone, if 'at' selected)", required: false
        input name: "ntfyAccessToken", type: "password", title: "Access Token (recommended; overrides username/password)", required: false
        input name: "ntfyUsername", type: "text", title: "Username (for servers with basic auth)", required: false
        input name: "ntfyPassword", type: "password", title: "Password (for servers with basic auth)", required: false
        input name: "ntfyIgnoreSsl", type: "bool", title: "Ignore SSL certificate errors (self-signed certificates)", defaultValue: false, required: false
        input name: "logEnable", type: "bool", title: "Enable Debug Logging (auto-disables after 30 minutes)", defaultValue: true
    }
}

void installed() {
    logDebug "Installed v${VERSION}"
    sendEvent(name: "lastNotificationStatus", value: "Not sent")
    sendEvent(name: "lastNotificationTime", value: "Never")
    sendEvent(name: "connectionStatus", value: "Unknown")
    scheduleDebugLogOff()
}

void updated() {
    logDebug "Preferences updated (v${VERSION})"
    warnIfSharedTopic()
    scheduleDebugLogOff()
}

void logsOff() {
    log.warn "Debug logging disabled"
    device.updateSetting("logEnable", [value: "false", type: "bool"])
}

void deviceNotification(String message) {
    String body = message?.trim()
    if (!body) {
        recordFailure("Notification", "Message cannot be empty")
        return
    }
    publish("Notification", body)
}

void testConnection() {
    log.info "Testing NTFY connection..."
    String body = "Connection test from Hubitat at ${timestamp()}"
    publish("Connection test", body) { Map headers ->
        headers.remove("Delay")
        headers.remove("At")
        headers["Tags"] = "white_check_mark,test_tube"
    }
}

/** 1.0.0 defaulted the topic to "hubitat". On the public server that topic is shared by everyone who kept the default. */
private void warnIfSharedTopic() {
    if (ntfyHost?.trim()?.equalsIgnoreCase(PUBLIC_HOST) && ntfyTopic?.trim() == LEGACY_DEFAULT_TOPIC) {
        log.warn "Topic '${LEGACY_DEFAULT_TOPIC}' on ${PUBLIC_HOST} is shared with every other user who kept the default: " +
                 "you will see their notifications and they will see yours. Set a unique, hard-to-guess topic."
    }
}

private void scheduleDebugLogOff() {
    if (logEnable) {
        runIn(DEBUG_LOG_AUTO_OFF_SECONDS, "logsOff")
    } else {
        unschedule("logsOff")
    }
}

/**
 * Validates configuration, builds the request and reports the outcome through attributes and logs.
 * A "Notification" updates the last-notification attributes; a "Connection test" only touches connectionStatus.
 */
private void publish(String operation, String body, Closure customizeHeaders = null) {
    List<String> problems = validateConfiguration()
    if (problems) {
        recordFailure(operation, "Configuration error: ${problems.join('; ')}", "Configuration Error")
        return
    }

    warnIfSharedTopic()
    Map headers = buildHeaders()
    if (customizeHeaders) customizeHeaders(headers)

    Map params = [
        uri: buildNotificationUri(),
        body: body,
        headers: headers,
        requestContentType: "text/plain",
        contentType: "text/plain",
        timeout: HTTP_TIMEOUT_SECONDS
    ]
    if (ntfyIgnoreSsl) params.ignoreSSLIssues = true

    logDebug "${operation}: POST ${params.uri}"
    logDebug "${operation}: headers ${redactHeaders(headers)}"
    logDebug "${operation}: body '${body}'"

    try {
        httpPost(params) { resp ->
            logDebug "${operation}: HTTP ${resp.status}"
            recordSuccess(operation, "HTTP ${resp.status}")
        }
    } catch (groovyx.net.http.HttpResponseException e) {
        recordFailure(operation, describeHttpError(e.statusCode, e.message), "Error")
    } catch (java.net.UnknownHostException e) {
        recordFailure(operation, "Unknown host '${ntfyHost}'. Check the hostname.", "Disconnected")
    } catch (java.net.ConnectException e) {
        recordFailure(operation, "Cannot connect to server '${ntfyHost}'. Check host and network connectivity.", "Disconnected")
    } catch (java.net.SocketTimeoutException e) {
        recordFailure(operation, "Connection timeout. Server may be slow or unreachable.", "Timeout")
    } catch (javax.net.ssl.SSLException e) {
        recordFailure(operation, "SSL error: ${e.message}. Enable 'Ignore SSL certificate errors' for self-signed certificates.", "Error")
    } catch (Exception e) {
        logDebug "${operation}: exception ${e}"
        recordFailure(operation, "Unexpected error: ${e.message}", "Error")
    }
}

private List<String> validateConfiguration() {
    List<String> errors = []

    String host = ntfyHost?.trim()
    if (!host) {
        errors << "NTFY Host is required"
    } else if (host.contains("://")) {
        errors << "NTFY Host must not include the protocol (use the Protocol setting)"
    }

    String topic = ntfyTopic?.trim()
    if (!topic) {
        errors << "Topic is required"
    } else {
        if (!topic.matches(/^[a-zA-Z0-9_-]+$/)) {
            errors << "Topic can only contain letters, numbers, underscores, and hyphens"
        }
        if (topic.length() > MAX_TOPIC_LENGTH) {
            errors << "Topic must be ${MAX_TOPIC_LENGTH} characters or fewer"
        }
    }

    if (!(ntfyProtocol in ["http", "https"])) {
        errors << "Protocol must be http or https"
    }

    if (hasPriority() && !(ntfyPriority in ["1", "2", "3", "4", "5"])) {
        errors << "Priority must be between 1 and 5"
    }

    boolean hasUsername = ntfyUsername?.trim()
    boolean hasPassword = ntfyPassword?.trim()
    if (hasUsername && !hasPassword) {
        errors << "Password is required when username is provided"
    }
    if (hasPassword && !hasUsername && !ntfyAccessToken?.trim()) {
        // 1.0.0 silently sent no auth in this case, so keep sending and just say so.
        log.warn "Password is set without a username, so no authentication is sent. Fill in the username, or use Access Token for token authentication."
    }

    if (ntfyClickAction && ntfyClickAction != "none") {
        String actionUrl = ntfyActionUrl?.trim()
        if (!actionUrl) {
            errors << "Action URL is required when click action is selected"
        } else if (!isHttpUrl(actionUrl)) {
            errors << "Action URL must start with http:// or https://"
        }
    }

    String attachUrl = ntfyAttachUrl?.trim()
    if (attachUrl && !isHttpUrl(attachUrl)) {
        errors << "Attachment URL must start with http:// or https://"
    }

    String iconUrl = ntfyIconUrl?.trim()
    if (iconUrl && !isHttpUrl(iconUrl)) {
        errors << "Icon URL must start with http:// or https://"
    }

    if (ntfyScheduling == "delay") {
        BigDecimal minutes = delayMinutes()
        if (minutes == null || minutes <= 0 || minutes > MAX_DELAY_MINUTES) {
            errors << "Delay minutes must be between 1 and ${MAX_DELAY_MINUTES} when delay scheduling is selected"
        }
    } else if (ntfyScheduling == "at") {
        errors.addAll(validateScheduleTime())
    }

    return errors
}

private List<String> validateScheduleTime() {
    String value = ntfyScheduleTime?.trim()
    if (!value) {
        return ["Schedule time is required when 'at' scheduling is selected"]
    }
    if (!value.matches(/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/)) {
        return ["Schedule time must be in format YYYY-MM-DD HH:MM:SS"]
    }
    Long scheduledEpochSeconds = parseScheduleTime(value)
    if (scheduledEpochSeconds == null) {
        return ["Schedule time '${value}' is not a valid date"]
    }
    long millisUntilScheduled = scheduledEpochSeconds * 1000 - now()
    if (millisUntilScheduled <= 0) {
        return ["Schedule time '${value}' must be in the future"]
    }
    if (millisUntilScheduled > MAX_DELAY_MINUTES * 60 * 1000L) {
        return ["Schedule time '${value}' must be within ${MAX_DELAY_MINUTES.intdiv(60 * 24)} days (ntfy's maximum)"]
    }
    return []
}

/** Interprets the configured schedule time in the hub's time zone. Returns epoch seconds, or null if unparseable. */
private Long parseScheduleTime(String value) {
    def format = new java.text.SimpleDateFormat(TIMESTAMP_FORMAT)
    format.lenient = false
    format.timeZone = location.timeZone
    try {
        return format.parse(value).time.intdiv(1000)
    } catch (java.text.ParseException ignored) {
        return null
    }
}

private String buildNotificationUri() {
    String host = ntfyHost.trim().replaceAll(/\/+$/, "")
    return "${ntfyProtocol}://${host}/${ntfyTopic.trim()}"
}

/** Maps preferences to ntfy publish headers. See https://docs.ntfy.sh/publish/ */
private Map buildHeaders() {
    Map headers = [:]

    String title = ntfyTitle?.trim()
    if (title) headers["Title"] = title

    if (hasPriority()) headers["Priority"] = ntfyPriority

    String tags = ntfyTags?.trim()
    if (tags) headers["Tags"] = tags

    String actionUrl = ntfyActionUrl?.trim()
    if (ntfyClickAction == "view") {
        headers["Click"] = actionUrl
    } else if (ntfyClickAction == "http") {
        headers["Actions"] = "http, Open, ${actionUrl}"
    }

    String attachUrl = ntfyAttachUrl?.trim()
    if (attachUrl) {
        headers["Attach"] = attachUrl
        String filename = ntfyAttachFilename?.trim()
        if (filename) headers["Filename"] = filename
    }

    String iconUrl = ntfyIconUrl?.trim()
    if (iconUrl) headers["Icon"] = iconUrl

    if (ntfyMarkdown) headers["Markdown"] = "yes"

    if (ntfyScheduling == "delay") {
        headers["Delay"] = "${delayMinutes()}m"
    } else if (ntfyScheduling == "at") {
        headers["At"] = parseScheduleTime(ntfyScheduleTime.trim()).toString()
    }

    String token = ntfyAccessToken?.trim()
    String username = ntfyUsername?.trim()
    if (token) {
        headers["Authorization"] = "Bearer ${token}"
    } else if (username && ntfyPassword?.trim()) {
        String credentials = "${username}:${ntfyPassword}".bytes.encodeBase64().toString()
        headers["Authorization"] = "Basic ${credentials}"
    }

    return headers
}

/** 1.0.0 offered a "Default" option whose stored value is the string "null"; treat it as unset. */
private boolean hasPriority() {
    return ntfyPriority && ntfyPriority != "null"
}

private BigDecimal delayMinutes() {
    return ntfyDelayMinutes == null || ntfyDelayMinutes == "" ? null : (ntfyDelayMinutes as BigDecimal)
}

private static boolean isHttpUrl(String url) {
    return url.startsWith("http://") || url.startsWith("https://")
}

private static Map redactHeaders(Map headers) {
    Map redacted = new LinkedHashMap(headers)
    if (redacted.Authorization) {
        redacted.Authorization = "${redacted.Authorization.split(' ')[0]} ***"
    }
    return redacted
}

private static String describeHttpError(int statusCode, String reason) {
    switch (statusCode) {
        case 400: return "Bad request (HTTP 400). Check your message format and topic name."
        case 401: return "Unauthorized (HTTP 401). Check your access token or username and password."
        case 403: return "Forbidden (HTTP 403). You don't have permission to publish to this topic."
        case 404: return "Not found (HTTP 404). Check your server host and topic name."
        case 413: return "Message too large (HTTP 413). Reduce message size."
        case 429: return "Rate limited (HTTP 429). Too many requests, try again later."
        case 500: return "Server error (HTTP 500). The NTFY server is experiencing issues."
        case 502:
        case 503:
        case 504: return "Server unavailable (HTTP ${statusCode}). The NTFY server is temporarily unavailable."
        default: return "HTTP ${statusCode}: ${reason}"
    }
}

private void recordSuccess(String operation, String detail) {
    boolean isNotification = operation == "Notification"
    if (isNotification) {
        logDebug "${operation} sent successfully (${detail})"
        sendEvent(name: "lastNotificationStatus", value: "Success")
        sendEvent(name: "lastNotificationTime", value: timestamp())
    } else {
        log.info "${operation} successful (${detail})"
    }
    sendEvent(name: "connectionStatus", value: "Connected")
}

private void recordFailure(String operation, String reason, String connectionStatus = null) {
    log.error "${operation} failed - ${reason}"
    if (operation == "Notification") {
        sendEvent(name: "lastNotificationStatus", value: "Failed")
        sendEvent(name: "lastNotificationTime", value: timestamp())
    }
    if (connectionStatus) {
        sendEvent(name: "connectionStatus", value: connectionStatus)
    }
}

private String timestamp() {
    return new Date().format(TIMESTAMP_FORMAT, location.timeZone)
}

private void logDebug(String message) {
    if (logEnable) log.debug message
}
