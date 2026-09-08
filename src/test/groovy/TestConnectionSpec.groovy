/**
 * The Test Connection command exercises the configured server and credentials without
 * counting as a notification and without inheriting scheduling that would delay the result.
 */
class TestConnectionSpec extends NtfyDriverSpecBase {

    def "sends a clearly marked test message using the configured server and auth"() {
        when:
        loadDriver(ntfyAccessToken: 'tk_abc').testConnection()

        then:
        with(lastPost()) {
            uri == 'https://ntfy.sh/hubitat-test-topic'
            body.startsWith('Connection test from Hubitat')
            headers.Tags == 'white_check_mark,test_tube'
            headers.Authorization == 'Bearer tk_abc'
        }
        attribute('connectionStatus') == 'Connected'
        logs.info.any { it.contains('Connection test successful') }
    }

    def "does not inherit delay or at scheduling"() {
        when:
        loadDriver(ntfyScheduling: 'delay', ntfyDelayMinutes: 30).testConnection()

        then:
        !lastPost().headers.containsKey('Delay')
        !lastPost().headers.containsKey('At')
    }

    def "does not touch the last-notification attributes"() {
        when:
        loadDriver().testConnection()

        then:
        attributeHistory('lastNotificationStatus').isEmpty()
        attributeHistory('lastNotificationTime').isEmpty()
    }

    def "a configuration error is reported without a request"() {
        when:
        loadDriver(ntfyTopic: null).testConnection()

        then:
        posts.isEmpty()
        attribute('connectionStatus') == 'Configuration Error'
        logs.error[0].startsWith('Connection test failed')
        logs.error[0].contains('Topic is required')
    }

    def "an auth failure is reported with connection status Error"() {
        given:
        def script = loadDriver(ntfyUsername: 'alice', ntfyPassword: 'wrong')
        failNextPostWith(httpError(401, 'Unauthorized'))

        when:
        script.testConnection()

        then:
        attribute('connectionStatus') == 'Error'
        logs.error[0].startsWith('Connection test failed')
        logs.error[0].contains('HTTP 401')
    }

    def "a network failure is reported with connection status Disconnected"() {
        given:
        def script = loadDriver()
        failNextPostWith(new UnknownHostException('ntfy.sh'))

        when:
        script.testConnection()

        then:
        attribute('connectionStatus') == 'Disconnected'
    }
}
