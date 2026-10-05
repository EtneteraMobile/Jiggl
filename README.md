# Jiggl - Jira & Toggl Tools for Chrome and Firefox
Jiggl is an extension for your browser that makes it easy to sync worklogs between Toggl and Jira - always for free.

Project is based on original [Toggl-to-jira](https://github.com/fyyyyy/Toggl-to-Jira-Chrome-Extension) extension.


##### Main features:
* Log Toggl time entries to Jira worklog.
* Automatic time round by user preferences.
* Support for multiple Jira servers.
* Start Toggl timer from Jira issue and merge request on Github, Gitlab, ... // TBD

**All contributors are welcome, see [Contributing section](#contributing).**


## Develop build
To build the extension, run the `build` gradle task. You can specify the target browser using the `browser` project property.

### For Chrome
```
./gradlew build -Pbrowser=chrome
```

### For Firefox
```
./gradlew build -Pbrowser=firefox
```

### Load Extension

#### Chrome
* go to `chrome://extensions`
* enable Developer mode in the top right corner
* click load unpacked and select your build folder `$PROJECT_DIR/build/extension`

#### Firefox
* go to `about:debugging`
* click "This Firefox"
* click "Load Temporary Add-on..."
* select any file in your build folder `$PROJECT_DIR/build/extension`

## Distribution build
To pack the extension as a zip archive, run the `bundle` task. Make sure to specify the target browser.

### For Chrome
```
./gradlew bundle -Pbrowser=chrome
```

### For Firefox
```
./gradlew bundle -Pbrowser=firefox
```

The archive is written to `build/distributions/Jiggl-<version>-<browser>.zip`.

## Building from source (for add-on reviewers)
The extension is written in Kotlin and compiled to JavaScript, so the shipped `Jiggl.js` is generated code. To reproduce the Firefox build from this source archive:

* Requirements: any OS with JDK 21 (tested with Eclipse Temurin 21) and internet access. The Gradle wrapper downloads Gradle 8.9, and the Kotlin/JS plugin downloads its own Node.js and Yarn. Dependency versions are pinned in `kotlin-js-store/yarn.lock`.
* Run:
  ```
  ./gradlew bundle -Pbrowser=firefox
  ```
* Output: the unpacked extension in `build/extension/` and the archive in `build/distributions/Jiggl-<version>-firefox.zip`.

## Release
Releases are published from a local machine by `scripts/release.sh`. It runs the tests, builds both browsers, uploads to the Chrome Web Store and Firefox Add-ons (with the source archive), then pushes the git tag and creates the GitHub Release with the changelog notes.

### One-time setup
1. Install Node.js (for `npx`) and the [GitHub CLI](https://cli.github.com/), then run `gh auth login`.
2. `cp .release.env.example .release.env`. The file is gitignored.
3. Firefox: create an API key at https://addons.mozilla.org/developers/addon/api/key/ and fill in `AMO_API_KEY` (JWT issuer) and `AMO_API_SECRET` (JWT secret).
4. Chrome: follow https://github.com/fregante/chrome-webstore-upload-keys to get `CWS_CLIENT_ID`, `CWS_CLIENT_SECRET` and `CWS_REFRESH_TOKEN`. Set the OAuth consent screen to "In production" (or "Internal"), otherwise the refresh token can expire. `CWS_PUBLISHER_ID` is in the [Developer Dashboard](https://chrome.google.com/webstore/devconsole) under Settings.

### Releasing a version
1. `scripts/bump-version.sh 0.5.1`: sets the version in `build.gradle` and both manifests, and moves the `[Unreleased]` changelog entries under the new version.
2. Review the diff, commit, and push `develop`.
3. `scripts/release.sh 0.5.1`: checks everything is consistent, shows the release notes, and asks for confirmation before publishing.

Both stores review new versions before they reach users, usually within a few days.

### When a release fails halfway
The stores reject a version they already have, so rerun only the steps that did not happen:
* Chrome failed: nothing was published or tagged. Fix it and rerun.
* Chrome succeeded, Firefox failed before the version appeared on AMO: `SKIP_CHROME=1 scripts/release.sh 0.5.1`.
* The version is on both stores (e.g. only the Firefox source upload failed): `SKIP_CHROME=1 SKIP_FIREFOX=1 scripts/release.sh 0.5.1` to just tag and create the GitHub Release. Upload the missing source archive (`build/distributions/Jiggl-<version>-source.zip`) by hand in the AMO Developer Hub.
* The tag is pushed but the GitHub Release is missing: run the script's last `gh release create` command by hand.

## Contributing
* Fell free to take any open [issue](https://github.com/EtneteraMobile/Jiggl/issues), ideally from upcoming milestone
* For new idea, please add new issue, so we can discuss it
##### Before sending PR
* Update changelog
* Make sure your changes are valid in develop build and doesn't break any tests