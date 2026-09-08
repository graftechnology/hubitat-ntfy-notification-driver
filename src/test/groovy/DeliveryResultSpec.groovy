import spock.lang.Unroll

/**
 * How the driver reports success and every class of failure through attributes and logs.
 */
class DeliveryResultSpec extends NtfyDriverSpecBase {

    def "a 2xx response marks the notification successful and the connection as connected"() {
        when:
        loadDriver().deviceNotification('hi')

        then:
        attribute('lastNotificationStatus') == 'Success'
        attribute('connectionStatus') == 'Connected'
        attribute('lastNotificationTime') ==~ /\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}/
        logs.error.isEmpty()
    }

    @Unroll
    def "HTTP #status is reported as '#expectedFragment'"() {
        given:
        def script = loadDriver()
        failNextPostWith(httpError(status, reason))

        when:
        script.deviceNotification('hi')

        then:
        attribute('lastNotificationStatus') == 'Failed'
        attribute('connectionStatus') == 'Error'
        logs.error.size() == 1
        logs.error[0].contains("HTTP ${status}")
        logs.error[0].contains(expectedFragment)

        where:
        status | reason                  | expectedFragment
        400    | 'Bad Request'           | 'Bad request'
        401    | 'Unauthorized'          | 'Check your access token or username and password'
        403    | 'Forbidden'             | 'permission to publish'
        404    | 'Not Found'             | 'Check your server host and topic name'
        413    | 'Payload Too Large'     | 'Message too large'
        429    | 'Too Many Requests'     | 'Rate limited'
        500    | 'Internal Server Error' | 'Server error'
        502    | 'Bad Gateway'           | 'temporarily unavailable'
        503    | 'Service Unavailable'   | 'temporarily unavailable'
        504    | 'Gateway Timeout'       | 'temporarily unavailable'
        418    | "I'm a teapot"          | "I'm a teapot"
    }

    @Unroll
    def "#description sets connection status '#expectedStatus'"() {
        given:
        def script = loadDriver()
        failNextPostWith(error)

        when:
        script.deviceNotification('hi')

        then:
        attribute('lastNotificationStatus') == 'Failed'
        attribute('connectionStatus') == expectedStatus
        logs.error.size() == 1
        logs.error[0].contains(expectedFragment)

        where:
        description             | error                                              | expectedStatus | expectedFragment
        'an unknown host'       | new UnknownHostException('ntfy.sh')                | 'Disconnected' | "Unknown host 'ntfy.sh'"
        'a refused connection'  | new ConnectException('refused')                    | 'Disconnected' | "Cannot connect to server 'ntfy.sh'"
        'a socket timeout'      | new SocketTimeoutException('read timed out')       | 'Timeout'      | 'timeout'
        'an SSL handshake error'| new javax.net.ssl.SSLHandshakeException('bad cert') | 'Error'        | 'Ignore SSL certificate errors'
        'an unexpected error'   | new IllegalStateException('boom')                  | 'Error'        | 'Unexpected error: boom'
    }

    def "a failure after a success records the failure"() {
        given:
        def script = loadDriver()
        script.deviceNotification('first')
        failNextPostWith(new ConnectException('refused'))

        when:
        script.deviceNotification('second')

        then:
        attributeHistory('lastNotificationStatus') == ['Success', 'Failed']
        attributeHistory('connectionStatus') == ['Connected', 'Disconnected']
    }

    def "credentials never appear in any log line"() {
        given:
        def script = loadDriver(ntfyUsername: 'alice', ntfyPassword: 'hunter2')
        def encoded = 'alice:hunter2'.bytes.encodeBase64().toString()

        when:
        script.deviceNotification('hi')

        then:
        posts.size() == 1
        allLogLines().every { !it.contains(encoded) && !it.contains('hunter2') }
        logs.debug.any { it.contains('Authorization') && it.contains('Basic ***') }
    }

    def "access tokens never appear in any log line"() {
        given:
        def script = loadDriver(ntfyAccessToken: 'tk_supersecret')

        when:
        script.deviceNotification('hi')

        then:
        allLogLines().every { !it.contains('tk_supersecret') }
        logs.debug.any { it.contains('Bearer ***') }
    }

    def "debug logging is silent when disabled"() {
        when:
        loadDriver(logEnable: false).deviceNotification('hi')

        then:
        logs.debug.isEmpty()
        posts.size() == 1
    }
}
