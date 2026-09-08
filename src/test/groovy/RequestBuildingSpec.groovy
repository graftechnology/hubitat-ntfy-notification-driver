import java.text.SimpleDateFormat

/**
 * The exact HTTP request the driver hands to Hubitat's httpPost: URI, body, ntfy headers and options.
 */
class RequestBuildingSpec extends NtfyDriverSpecBase {

    def "sends the trimmed message as plain text to protocol://host/topic"() {
        when:
        loadDriver().deviceNotification('  Motion detected  ')

        then:
        with(lastPost()) {
            uri == 'https://ntfy.sh/hubitat-test-topic'
            body == 'Motion detected'
            requestContentType == 'text/plain'
            contentType == 'text/plain'
            timeout == 30
            !containsKey('ignoreSSLIssues')
        }
    }

    def "a minimal configuration sends only Title and Priority"() {
        when:
        loadDriver().deviceNotification('hello')

        then:
        lastPost().headers == [Title: 'Hubitat', Priority: '3']
    }

    def "trims and normalizes host and topic"() {
        when:
        loadDriver(ntfyProtocol: 'http', ntfyHost: ' ntfy.example.com:8080/ ', ntfyTopic: ' alerts ').deviceNotification('hi')

        then:
        lastPost().uri == 'http://ntfy.example.com:8080/alerts'
    }

    def "omits the title when blank"() {
        when:
        loadDriver(ntfyTitle: '  ').deviceNotification('hi')

        then:
        !lastPost().headers.containsKey('Title')
    }

    def "maps tags, icon and markdown to ntfy headers"() {
        when:
        loadDriver(ntfyTags: ' warning,house ', ntfyIconUrl: 'https://x/icon.png', ntfyMarkdown: true).deviceNotification('**hi**')

        then:
        with(lastPost().headers) {
            Tags == 'warning,house'
            Icon == 'https://x/icon.png'
            Markdown == 'yes'
        }
    }

    def "an 'Open URL' click action sets the Click header"() {
        when:
        loadDriver(ntfyClickAction: 'view', ntfyActionUrl: ' https://hub.local/device/1 ').deviceNotification('hi')

        then:
        lastPost().headers.Click == 'https://hub.local/device/1'
        !lastPost().headers.containsKey('Actions')
    }

    def "an 'HTTP Request' click action sets an http action button"() {
        when:
        loadDriver(ntfyClickAction: 'http', ntfyActionUrl: 'https://hub.local/api').deviceNotification('hi')

        then:
        lastPost().headers.Actions == 'http, Open, https://hub.local/api'
        !lastPost().headers.containsKey('Click')
    }

    def "attachments send Attach and, when given, Filename"() {
        when:
        loadDriver(ntfyAttachUrl: 'http://cam.local/snap.jpg', ntfyAttachFilename: filename).deviceNotification('hi')

        then:
        lastPost().headers.Attach == 'http://cam.local/snap.jpg'
        lastPost().headers.Filename == expectedFilename
        lastPost().headers.containsKey('Filename') == (expectedFilename != null)

        where:
        filename        | expectedFilename
        'front.jpg'     | 'front.jpg'
        '  '            | null
        null            | null
    }

    def "a filename without an attachment is ignored"() {
        when:
        loadDriver(ntfyAttachFilename: 'front.jpg').deviceNotification('hi')

        then:
        !lastPost().headers.containsKey('Filename')
    }

    def "delay scheduling sends a minutes duration"() {
        when:
        loadDriver(ntfyScheduling: 'delay', ntfyDelayMinutes: minutes).deviceNotification('hi')

        then:
        lastPost().headers.Delay == expected
        !lastPost().headers.containsKey('At')

        where:
        minutes | expected
        30      | '30m'
        4320    | '4320m'
        1.5     | '1.5m'
    }

    def "at scheduling sends a unix timestamp interpreted in the hub's time zone"() {
        given:
        def format = new SimpleDateFormat('yyyy-MM-dd HH:mm:ss')
        format.timeZone = HUB_TIME_ZONE
        long expectedEpochSeconds = format.parse('2099-01-02 03:04:05').time.intdiv(1000)

        when:
        loadDriver(ntfyScheduling: 'at', ntfyScheduleTime: ' 2099-01-02 03:04:05 ').deviceNotification('hi')

        then:
        lastPost().headers.At == expectedEpochSeconds.toString()
        !lastPost().headers.containsKey('Delay')
    }

    def "scheduling values are ignored when scheduling is off"() {
        when:
        loadDriver(ntfyScheduling: 'none', ntfyDelayMinutes: 30, ntfyScheduleTime: '2099-01-01 00:00:00').deviceNotification('hi')

        then:
        !lastPost().headers.containsKey('Delay')
        !lastPost().headers.containsKey('At')
    }

    def "username and password become a Basic authorization header"() {
        when:
        loadDriver(ntfyUsername: ' alice ', ntfyPassword: 'p@ss:word').deviceNotification('hi')

        then:
        lastPost().headers.Authorization == 'Basic ' + 'alice:p@ss:word'.bytes.encodeBase64().toString()
    }

    def "an access token becomes a Bearer authorization header"() {
        when:
        loadDriver(ntfyAccessToken: ' tk_abc123 ').deviceNotification('hi')

        then:
        lastPost().headers.Authorization == 'Bearer tk_abc123'
    }

    def "an access token takes precedence over username and password"() {
        when:
        loadDriver(ntfyAccessToken: 'tk_abc123', ntfyUsername: 'alice', ntfyPassword: 'secret').deviceNotification('hi')

        then:
        lastPost().headers.Authorization == 'Bearer tk_abc123'
    }

    def "no authorization header is sent without credentials"() {
        when:
        loadDriver().deviceNotification('hi')

        then:
        !lastPost().headers.containsKey('Authorization')
    }

    def "ignoring SSL errors is passed through only when enabled"() {
        when:
        loadDriver(ntfyIgnoreSsl: true).deviceNotification('hi')

        then:
        lastPost().ignoreSSLIssues == true
    }

    def "every priority level is sent verbatim"() {
        when:
        loadDriver(ntfyPriority: level).deviceNotification('hi')

        then:
        lastPost().headers.Priority == level

        where:
        level << ['1', '2', '3', '4', '5']
    }
}
