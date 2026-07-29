# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Jiggl is a Chrome/Firefox browser extension (Kotlin/JS, not a JVM project) that syncs Toggl time entries to Jira worklogs. `AGENTS.md` contains the same contributor guidelines and should be kept in sync with this file.

## Commands

```bash
./gradlew build -Pbrowser=chrome    # build unpacked extension into build/extension/
./gradlew build -Pbrowser=firefox   # same, with Firefox manifest
./gradlew bundle -Pbrowser=chrome   # zip for distribution into build/distributions/
./gradlew test                      # Kotlin/JS tests (runs in a headless browser via Karma)
./gradlew test --tests "utils.extensions.DateExtensionsTest"   # single test class
```

`-Pbrowser` defaults to `chrome`. It only selects which `src/main/resources/manifest-<browser>.json` gets copied to `manifest.json` (the `copyManifest` task); the compiled code is identical. Load the unpacked extension from `build/extension/` (Chrome: `chrome://extensions`; Firefox: `about:debugging` → This Firefox → Load Temporary Add-on).

Toolchain: old `org.jetbrains.kotlin.js` Gradle plugin, Kotlin 1.9.10, kotlinx.serialization, Ktor client, kotlinx-html. Yarn lockfile lives in `kotlin-js-store/` — commit it when dependencies change.

## Architecture

**Single bundle, path-dispatched entry point.** The whole extension compiles to one JS file (`Jiggl.js`) loaded by every extension page. `src/main/kotlin/App.kt#main()` inspects `window.location.pathname` and routes to `Popup` (`/html/popup.html`), `Options` (`/html/options.html`), or `Background`. Before dispatch it loads preferences and inits `JiraApi`/`TogglApi`, so those singletons are always initialized when page code runs.

**UI is built imperatively in Kotlin.** `popup/Popup.kt` and `options/Options.kt` grab elements from the static HTML in `src/main/resources/html/` and render rows with kotlinx-html DOM builders. There is no framework; state lives in the page classes (e.g. `Popup.logs: MutableList<WorkLog>`).

**Browser APIs via hand-written externals.** `src/main/kotlin/browser/` holds external declarations for `browser.storage`, `browser.tabs`, `browser.permissions`. They target the promise-based `browser.*` namespace; `js/browser-polyfill.js` makes that work on Chrome. Plain JS objects are created with the `js("({})")` / external-interface pattern (see `Preferences` in `AppPreferences.kt`) — these are not serializable Kotlin classes.

**Preferences** (`AppPreferences.kt`) are stored in `browser.storage.sync` as a plain JS object. Multi-Jira support works by mapping Toggl project id → Jira URL (`jiraUrls`); `jiraUrl` is the default server. Toggl entry descriptions are parsed into issue key + comment by the user-configurable `togglTemplate` regex with named groups (`utils/js/regexp.kt` wraps JS RegExp because Kotlin's Regex lacks named-group support here; parsing lives in `popup/models/WorkLog.fromTemplate`).

**HTTP has two paths.** GETs (Toggl API, Jira user/worklog reads) go through Ktor's JS client with cookie/token auth. The Jira worklog POST (`JiraApi.logWork`) is special: it first tries messaging the content script (`resources/js/content.js`, injected into Jira tabs, message type `jiggl/logWork`) to perform a same-origin fetch — required on Firefox, where cross-origin extension POSTs trip Jira's XSRF protection — and falls back to a direct `window.fetch` with credentials, which works in Chrome. When testing Jira logging in Firefox, a Jira tab must be open.

**Manifests.** Chrome (MV3 service worker via `js/background.js` importScripts shim) and Firefox (background scripts array) differ only in the background section. Keep permissions and content-script matches in sync across both manifests when touching either, and add host permissions only when required.

## Conventions

- Tests mirror main package paths under `src/test/kotlin/` (note the flat `utils.extensions/` directory naming), named `ThingBeingTestedTest.kt`, using `kotlin-test-js`. Logic in `utils/` and `popup/models/` is expected to have tests.
- Update `CHANGELOG.md` for user-visible changes; version is duplicated in `build.gradle` and both manifests.
- The Toggl API token comes from extension storage — never hardcode it.
