/**
 * Hub lifecycle hooks: installed(), updated() and the debug-logging auto-off convention.
 */
class LifecycleSpec extends NtfyDriverSpecBase {

    def "installing seeds the status attributes"() {
        when:
        loadDriver().installed()

        then:
        attribute('lastNotificationStatus') == 'Not sent'
        attribute('lastNotificationTime') == 'Never'
        attribute('connectionStatus') == 'Unknown'
    }

    def "installing schedules debug logging to switch off after 30 minutes"() {
        when:
        loadDriver().installed()

        then:
        1 * api.runIn(1800L, 'logsOff')
    }

    def "saving preferences keeps the existing status history"() {
        given:
        def script = loadDriver()
        script.deviceNotification('hi')

        when:
        script.updated()

        then:
        attribute('lastNotificationStatus') == 'Success'
        attribute('connectionStatus') == 'Connected'
        attributeHistory('lastNotificationTime').every { it != 'Never' }
    }

    def "saving preferences with debug logging on schedules the auto-off"() {
        when:
        loadDriver(logEnable: true).updated()

        then:
        1 * api.runIn(1800L, 'logsOff')
        0 * api.unschedule('logsOff')
    }

    def "saving preferences with debug logging off cancels any pending auto-off"() {
        when:
        loadDriver(logEnable: false).updated()

        then:
        0 * api.runIn(*_)
        1 * api.unschedule('logsOff')
    }

    def "saving preferences with the shared default topic on ntfy.sh warns the user"() {
        when:
        loadDriver(ntfyHost: 'ntfy.sh', ntfyTopic: 'hubitat').updated()

        then:
        logs.warn.size() == 1
        logs.warn[0].contains("'hubitat'")
    }

    def "logsOff turns the preference off and says so"() {
        when:
        loadDriver().logsOff()

        then:
        1 * device.updateSetting('logEnable', [value: 'false', type: 'bool'])
        logs.warn.any { it.contains('Debug logging disabled') }
    }
}
