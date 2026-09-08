import spock.lang.Unroll

/**
 * Configuration errors must never reach the network. They fail fast with an actionable message,
 * mark the notification as failed and flag the connection status.
 */
class ConfigurationValidationSpec extends NtfyDriverSpecBase {

    @Unroll
    def "rejects #description"() {
        given:
        def script = loadDriver(settings)

        when:
        script.deviceNotification('hello')

        then:
        posts.isEmpty()
        attribute('lastNotificationStatus') == 'Failed'
        attribute('connectionStatus') == 'Configuration Error'
        logs.error.size() == 1
        logs.error[0].contains(expectedMessage)

        where:
        description                                | settings                                                              | expectedMessage
        'a missing host'                           | [ntfyHost: null]                                                      | 'NTFY Host is required'
        'a blank host'                             | [ntfyHost: '   ']                                                     | 'NTFY Host is required'
        'a host that includes the protocol'        | [ntfyHost: 'https://ntfy.sh']                                         | 'NTFY Host must not include the protocol'
        'a missing topic'                          | [ntfyTopic: null]                                                     | 'Topic is required'
        'a topic with invalid characters'          | [ntfyTopic: 'my topic!']                                              | 'letters, numbers, underscores, and hyphens'
        'a topic longer than 64 characters'        | [ntfyTopic: 'a' * 65]                                                 | '64 characters'
        'an unknown protocol'                      | [ntfyProtocol: 'ftp']                                                 | 'Protocol must be http or https'
        'a priority outside 1-5'                   | [ntfyPriority: '9']                                                   | 'Priority must be between 1 and 5'
        'a username without a password'            | [ntfyUsername: 'alice']                                               | 'Password is required when username is provided'
        'a password without a username'            | [ntfyPassword: 's3cret']                                              | 'Username is required when a password is provided'
        'a click action without a URL'             | [ntfyClickAction: 'view']                                             | 'Action URL is required'
        'a click action with a non-http URL'       | [ntfyClickAction: 'view', ntfyActionUrl: 'ftp://x']                   | 'Action URL must start with http:// or https://'
        'an attachment with a non-http URL'        | [ntfyAttachUrl: 'file:///etc/passwd']                                 | 'Attachment URL must start with http:// or https://'
        'an icon with a non-http URL'              | [ntfyIconUrl: 'icon.png']                                             | 'Icon URL must start with http:// or https://'
        'delay scheduling without minutes'         | [ntfyScheduling: 'delay']                                             | 'Delay minutes must be between 1 and 4320'
        'delay scheduling with zero minutes'       | [ntfyScheduling: 'delay', ntfyDelayMinutes: 0]                        | 'Delay minutes must be between 1 and 4320'
        'delay scheduling beyond three days'       | [ntfyScheduling: 'delay', ntfyDelayMinutes: 4321]                     | 'Delay minutes must be between 1 and 4320'
        'at scheduling without a time'             | [ntfyScheduling: 'at']                                                | 'Schedule time is required'
        'at scheduling with a malformed time'      | [ntfyScheduling: 'at', ntfyScheduleTime: 'tomorrow 8am']              | 'YYYY-MM-DD HH:MM:SS'
        'at scheduling with an impossible date'    | [ntfyScheduling: 'at', ntfyScheduleTime: '2099-02-31 08:00:00']       | 'not a valid date'
        'at scheduling with a time in the past'    | [ntfyScheduling: 'at', ntfyScheduleTime: '2020-01-01 08:00:00']       | 'must be in the future'
        'at scheduling more than three days out'   | [ntfyScheduling: 'at', ntfyScheduleTime: '2099-01-01 08:00:00']       | 'must be within 3 days'
    }

    def "reports every configuration problem at once"() {
        given:
        def script = loadDriver(ntfyHost: null, ntfyTopic: null)

        when:
        script.deviceNotification('hello')

        then:
        logs.error[0].contains('NTFY Host is required')
        logs.error[0].contains('Topic is required')
    }

    def "tolerates the legacy 'null' priority value stored by 1.0.0"() {
        given:
        def script = loadDriver(ntfyPriority: 'null')

        when:
        script.deviceNotification('hello')

        then:
        posts.size() == 1
        !lastPost().headers.containsKey('Priority')
    }

    def "an empty message fails without a request"() {
        given:
        def script = loadDriver()

        when:
        script.deviceNotification(message)

        then:
        posts.isEmpty()
        attribute('lastNotificationStatus') == 'Failed'
        logs.error[0].contains('Message cannot be empty')

        where:
        message << [null, '', '   ', '\n\t']
    }

    def "an empty message does not disturb an existing connection status"() {
        given:
        def script = loadDriver()
        script.deviceNotification('first')

        when:
        script.deviceNotification('')

        then:
        attribute('connectionStatus') == 'Connected'
    }

    def "a configuration error after a successful send flips the connection status"() {
        given:
        def script = loadDriver(ntfyTopic: 'valid-topic')
        script.deviceNotification('first')

        when:
        // Simulates the user saving a bad topic: the driver reads preferences live on every send.
        def brokenScript = loadDriver(ntfyTopic: null)
        brokenScript.deviceNotification('second')

        then:
        attributeHistory('connectionStatus') == ['Connected', 'Configuration Error']
        attributeHistory('lastNotificationStatus') == ['Success', 'Failed']
    }
}
