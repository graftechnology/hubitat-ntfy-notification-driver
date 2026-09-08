import groovyx.net.http.HttpResponseDecorator
import groovyx.net.http.HttpResponseException
import me.biocomp.hubitat_ci.api.common_api.DeviceWrapper
import me.biocomp.hubitat_ci.api.common_api.Location
import me.biocomp.hubitat_ci.api.common_api.Log
import me.biocomp.hubitat_ci.api.device_api.DeviceExecutor
import me.biocomp.hubitat_ci.device.HubitatDeviceSandbox
import me.biocomp.hubitat_ci.device.HubitatDeviceScript
import me.biocomp.hubitat_ci.validation.Flags
import org.apache.http.HttpVersion
import org.apache.http.message.BasicHttpResponse
import org.apache.http.message.BasicStatusLine
import spock.lang.Specification

/**
 * Shared harness for driver specs. Loads the real driver file into the hubitat_ci sandbox
 * with a mocked hub API, and records every event, HTTP request and log line the driver emits.
 */
abstract class NtfyDriverSpecBase extends Specification {

    /**
     * Mocked as a class rather than the raw interface: Spock proxies interfaces with a JDK dynamic proxy, which wraps
     * the checked IOExceptions Hubitat's httpPost throws (HttpResponseException, UnknownHostException...) in
     * UndeclaredThrowableException. A class proxy lets them reach the driver's catch blocks unchanged.
     */
    static abstract class HubApi implements DeviceExecutor {}

    static final File DRIVER_FILE = new File('drivers/graftechnology/ntfy-notifier.groovy')
    static final TimeZone HUB_TIME_ZONE = TimeZone.getTimeZone('America/Chicago')

    /** The hub's settings for a freshly configured device. Every preference is listed so the sandbox never synthesizes a value. */
    static final Map BASE_SETTINGS = [
        ntfyProtocol      : 'https',
        ntfyHost          : 'ntfy.sh',
        ntfyTopic         : 'hubitat-test-topic',
        ntfyTitle         : 'Hubitat',
        ntfyPriority      : '3',
        ntfyTags          : null,
        ntfyClickAction   : 'none',
        ntfyActionUrl     : null,
        ntfyAttachUrl     : null,
        ntfyAttachFilename: null,
        ntfyIconUrl       : null,
        ntfyMarkdown      : false,
        ntfyScheduling    : 'none',
        ntfyDelayMinutes  : null,
        ntfyScheduleTime  : null,
        ntfyAccessToken   : null,
        ntfyUsername      : null,
        ntfyPassword      : null,
        ntfyIgnoreSsl     : false,
        logEnable         : true,
    ].asImmutable()

    List<Map> events = []
    List<Map> posts = []
    /** What the hub's httpPost does. Defaults to a 200 response; tests swap it to simulate failures. */
    Closure postBehavior = { Map params, Closure handler -> handler.call(okResponse()) }
    Map<String, List<String>> logs = [debug: [], info: [], warn: [], error: [], trace: []]

    Log log = Mock {
        _ * debug(_) >> { args -> logs.debug << (args[0] as String) }
        _ * info(_) >> { args -> logs.info << (args[0] as String) }
        _ * warn(_) >> { args -> logs.warn << (args[0] as String) }
        _ * error(_) >> { args -> logs.error << (args[0] as String) }
        _ * trace(_) >> { args -> logs.trace << (args[0] as String) }
    }
    DeviceWrapper device = Mock()
    Location location = Mock {
        _ * getTimeZone() >> HUB_TIME_ZONE
    }
    HubApi api = Mock {
        _ * getLog() >> log
        _ * getDevice() >> device
        _ * getLocation() >> location
        _ * now() >> { System.currentTimeMillis() }
        _ * sendEvent(_) >> { args -> events << (args[0] as Map) }
        _ * httpPost(_, _) >> { args -> posts << (args[0] as Map); postBehavior.call(args[0] as Map, args[1] as Closure) }
    }

    /** Load the driver with the given preference overrides on top of {@link #BASE_SETTINGS}. */
    HubitatDeviceScript loadDriver(Map settingOverrides = [:]) {
        new HubitatDeviceSandbox(DRIVER_FILE).run(
            api: api,
            userSettingValues: BASE_SETTINGS + settingOverrides,
            validationFlags: [Flags.DontRestrictGroovy, Flags.DontRequireParseMethodInDevice],
        )
    }

    static Map okResponse(int status = 200) {
        [status: status, headers: [:], data: [id: 'abc123']]
    }

    /** Builds the exception Hubitat's httpPost throws for a non-2xx response. */
    static HttpResponseException httpError(int status, String reason) {
        def response = new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, status, reason))
        new HttpResponseException(new HttpResponseDecorator(response, null))
    }

    void failNextPostWith(Throwable error) {
        postBehavior = { Map params, Closure handler -> throw error }
    }

    String attribute(String name) {
        events.reverse().find { it.name == name }?.value
    }

    List<String> attributeHistory(String name) {
        events.findAll { it.name == name }*.value
    }

    Map lastPost() {
        posts ? posts.last() : null
    }

    List<String> allLogLines() {
        logs.values().flatten() as List<String>
    }
}
