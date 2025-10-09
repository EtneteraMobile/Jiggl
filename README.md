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

## Contributing
* Fell free to take any open [issue](https://github.com/EtneteraMobile/Jiggl/issues), ideally from upcoming milestone
* For new idea, please add new issue, so we can discuss it
##### Before sending PR
* Update changelog
* Make sure your changes are valid in develop build and doesn't break any tests