/**
 * Guards the driver's public surface. Existing installs are matched by name/namespace and keep
 * their stored preference values, so nothing in here may change without a migration plan.
 */
class MetadataSpec extends NtfyDriverSpecBase {

    /** Every preference name shipped in 1.0.0. Removing or renaming one silently discards users' saved values. */
    static final List<String> V1_PREFERENCE_NAMES = [
        'ntfyProtocol', 'ntfyHost', 'ntfyTopic', 'ntfyTitle', 'ntfyPriority', 'ntfyTags',
        'ntfyClickAction', 'ntfyActionUrl', 'ntfyAttachUrl', 'ntfyAttachFilename', 'ntfyIconUrl',
        'ntfyMarkdown', 'ntfyScheduling', 'ntfyDelayMinutes', 'ntfyScheduleTime',
        'ntfyUsername', 'ntfyPassword', 'logEnable',
    ]

    def "driver identity matches the HPM manifest so updates reach existing installs"() {
        given:
        def script = loadDriver()
        def manifest = new groovy.json.JsonSlurper().parse(new File('packageManifest.json'))

        expect:
        script.producedDefinition.options.name == 'NTFY Notifier Driver'
        script.producedDefinition.options.namespace == 'graftechnology'
        script.producedDefinition.options.author == 'Graf Technology, LLC'
        script.producedDefinition.options.importUrl == manifest.drivers[0].location
        manifest.drivers[0].name == 'NTFY Notifier Driver'
        manifest.drivers[0].namespace == 'graftechnology'
    }

    def "driver version constant agrees with the package manifest"() {
        given:
        def script = loadDriver()
        def manifest = new groovy.json.JsonSlurper().parse(new File('packageManifest.json'))

        expect:
        script.VERSION == manifest.version
        script.VERSION == manifest.drivers[0].version
    }

    def "implements the Notification capability plus the test command and status attributes"() {
        given:
        def script = loadDriver()
        def definition = script.producedDefinition

        expect:
        definition.capabilities == ['Notification']
        definition.commands*.name == ['testConnection']
        definition.attributes*.name.sort() == ['connectionStatus', 'lastNotificationStatus', 'lastNotificationTime']
    }

    def "every 1.0.0 preference still exists"() {
        given:
        def names = loadDriver().producedPreferences*.readName()

        expect:
        V1_PREFERENCE_NAMES.every { it in names }
    }

    def "new 1.1.0 preferences are present"() {
        given:
        def names = loadDriver().producedPreferences*.readName()

        expect:
        'ntfyAccessToken' in names
        'ntfyIgnoreSsl' in names
    }

    def "topic has no default so users on the public server cannot share a topic by accident"() {
        given:
        def topic = loadDriver().producedPreferences.find { it.readName() == 'ntfyTopic' }

        expect:
        topic.options.required == true
        topic.options.defaultValue == null
    }

    def "priority options are exactly ntfy's five levels"() {
        given:
        def priority = loadDriver().producedPreferences.find { it.readName() == 'ntfyPriority' }

        expect:
        priority.options.options.keySet() as List == ['1', '2', '3', '4', '5']
        priority.options.defaultValue == '3'
    }

    def "secrets use password inputs"() {
        given:
        def prefs = loadDriver().producedPreferences

        expect:
        prefs.find { it.readName() == 'ntfyPassword' }.readType() == 'password'
        prefs.find { it.readName() == 'ntfyAccessToken' }.readType() == 'password'
    }
}
