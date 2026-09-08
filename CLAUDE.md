# NTFY Notifier Driver for Hubitat

Open-source Hubitat Elevation driver (Groovy) that publishes notifications to ntfy. Distributed via
Hubitat Package Manager (HPM) from the Graf Technology HPM repository and used by real hubs today, so
backward compatibility with installed devices is the first constraint on every change.

## Layout

- `drivers/graftechnology/ntfy-notifier.groovy`: the entire product. One file, installed on hubs verbatim.
- `packageManifest.json`: HPM manifest. `version`, `dateReleased`, `releaseNotes` and the driver entry's
  `version` must be bumped together with the driver's `VERSION` constant. CI fails if they disagree.
- `src/test/groovy/`: Spock specs run through hubitat_ci. `NtfyDriverSpecBase` is the harness; every spec
  loads the real driver file with explicit settings and asserts on recorded events, requests and logs.
- `CHANGELOG.md`: Keep a Changelog format. Add an entry for every user-visible change.

## Running tests

```
./gradlew test
```

Gradle needs JDK 17+ (`JAVA_HOME=/opt/homebrew/opt/openjdk@21` on this machine). The JDK 11 test
toolchain is auto-provisioned. hubitat_ci 0.17 comes from an anonymous Azure Maven feed; newer versions
only exist on GitHub Packages, which needs a token, so stay on 0.17 unless that changes.

## Compatibility rules

- Never rename or remove a preference. Hubitat keeps saved values by name; a rename silently discards them.
  `MetadataSpec` lists the 1.0.0 names and fails if one disappears.
- Never change `name` or `namespace` in `definition()`, or the driver `id`/`location` in the manifest.
  HPM and the hub match installed drivers on these.
- Tolerate legacy stored values (example: priority `"null"` from the 1.0.0 "Default" option).
- Adding preferences is fine. Defaults only apply to new devices.

## Hubitat and ntfy facts worth remembering

- `httpPost` throws `groovyx.net.http.HttpResponseException` for non-2xx, so the success closure only
  runs for 2xx. `requestContentType` is the request Content-Type; `contentType` is the Accept/response type.
- `definition()` accepts only `name`, `namespace`, `author`, `importUrl`. hubitat_ci enforces this.
- Groovy map literal `[null: "x"]` produces the string key `"null"`.
- ntfy scheduling accepts durations, Unix timestamps and English natural language only. Absolute
  `YYYY-MM-DD HH:MM:SS` is not parsed as a date, so the driver converts it to a Unix timestamp itself.
- ntfy topics: `^[-_A-Za-z0-9]{1,64}$`. On ntfy.sh a topic is public to anyone who knows its name, so the
  driver ships no default topic.
- ntfy auth: `Authorization: Bearer tk_...` or Basic. Token wins when both are configured.

## Testing gotchas

- hubitat_ci synthesizes placeholder values for any preference not passed in `userSettingValues`, so the
  harness passes every preference explicitly (nulls included).
- Mock the executor as the abstract `HubApi` class, not the interface. Spock's interface proxy wraps
  checked exceptions in `UndeclaredThrowableException` and the driver's catch blocks never see them.
- Spock picks the earliest declared matching interaction, so per-test HTTP failures go through the
  swappable `postBehavior` closure instead of re-declaring `httpPost`.
- `Flags.DontRestrictGroovy` is required: the 0.17 sandbox whitelist rejects `e.statusCode`, which Hubitat allows.

## Release checklist

1. Bump `VERSION` in the driver and the three version-related fields in `packageManifest.json`.
2. Update `CHANGELOG.md`.
3. `./gradlew test` green.
4. Commit, tag `vX.Y.Z`, push, create a GitHub release. HPM serves whatever is on `main`.
