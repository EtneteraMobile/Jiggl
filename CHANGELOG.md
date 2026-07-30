# Changelog

## [Unreleased]
### Added
- Multiple Jira servers routed by Jira project key: each additional server lists the project keys (e.g. "ABC, INT") it handles; everything else goes to the default server
- Issue links in the popup now point to the server the issue is logged to
- Each entry in the popup shows the Jira host it will be logged to (when more than one server is configured)
- Popup header lists the signed-in account per Jira host on its own line instead of next to the app name

### Changed
- Additional Jira servers are no longer mapped by Toggl project; existing servers are migrated and need their project keys filled in on the options page
- Saving options now requests host permission for all configured Jira servers, not just the default one

### Fixed
- Already-logged check ("OK" mark) broken when Jira omits the author email from worklogs (e.g. after a privacy-settings change): the whole worklog response failed to parse and the check silently did nothing

## [0.4.4] - 2025-08-28
### Added
- Firefox implementation

## [0.3.3] - 2024-05-05
### Fixed
- issue/59 - Toggl api v8 -> v9

## [0.3.2] - 2021-11-08
### Fixed
- issue/54 - Jira and Toggl matched with year, month and date
- issue/56 - Update Toggl api url.

## [0.3.1] - 2021-09-06
### Fixed
- Toggl worklog id updated from Int to Long

## [0.3.0] - unreleased
### Added
- Highlight invalid issues in the popup
- Template for parsing Toggl entries
- Added own version of js RegExp api

### Changed
- Migrated to Kotlin 1.4

### Removed

### Fixed

### Deprecated

### Security

## [0.2.0] - 2020-10-19
### Added
- Option to check and uncheck all issues
- Filtering of issues in popup
- Default log description if empty
- Support for multiple Jira servers

### Changed
- Rewritten to Kotlin
- Fetch entries from Toggl including the end date
- Disable log button until everything is ready

### Fixed
- Toggl timezone handling

## [0.1.0]
### Added
- Option to merge entries by issue, date and description

### Changed
- Directory structure

### Fixed
- Round after issues merge
- No need to reload extension after Toggl api token change

## [Toggl-to-JIRA 2.1.7]
- Initial fork from https://github.com/fyyyyy/Toggl-to-Jira-Chrome-Extension