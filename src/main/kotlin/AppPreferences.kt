import browser.storage.sync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.await
import kotlinx.coroutines.launch

/**
 * Object for accessing app-wide preferences.
 */
object AppPreferences {

    suspend fun getPreferences(): Preferences {
        val result = sync.get(getDefaults()).await()
        return migrateLegacyJiraUrls(result.unsafeCast<Preferences>())
    }

    fun setPreferences(prefs: Preferences): Job =
        GlobalScope.launch(Dispatchers.Default) {
            // Ensure we wait for the write to complete for consistency across browsers
            sync.set(prefs).await()
        }

    /**
     * Older versions mapped Toggl project id -> Jira url in [Preferences.jiraUrls].
     * Carry those urls over as servers without project keys so the user only
     * has to fill in the keys on the options page; they never match until then.
     */
    private fun migrateLegacyJiraUrls(prefs: Preferences): Preferences {
        if (prefs.jiraServers.isEmpty() && prefs.jiraUrls.isNotEmpty()) {
            prefs.jiraServers = prefs.jiraUrls.map { "" to it.second }.toTypedArray()
        }
        return prefs
    }

    private fun getDefaults(): Preferences =
        Preferences {
            jiraUrl = "https://jira.atlassian.net"
            jiraUrls = arrayOf()
            jiraServers = arrayOf()
            mergeEntriesBy = "no-merge"
            jumpToToday = false
            togglApiToken = ""
            togglTemplate = "(?<issue>.*?-\\d+) ?(?<desc>.*)"
            roundType = "no-round"
            roundValue = 15
            defaultComment = "Logged by Jiggl Chrome Extension"
        }
}

/**
 * Model class for app preferences.
 *
 * @property jiraUrl Base JIRA url, used when no server in [jiraServers] matches.
 * @property jiraServers Additional Jira servers as comma-separated Jira project keys (e.g. "ABC, INT") -> url.
 * @property jiraUrls Legacy mapping (Toggl project id -> url), only read to migrate into [jiraServers].
 * @property mergeEntriesBy Selected merging options of toggl entries.
 *                          Possible values are `no-merge`, `issue-only`, `issue-and-date`, `issue-and-date-and-desc`.
 * @property jumpToToday Flag whether date picker should show today date by default.
 * @property togglApiToken User`s Toggl API token.
 * @property togglTemplate Template for parsing Toggl entries.
 * @property roundType Possible values are `no-round`, `round-up`, `natural-round`, `smart-round`.
 * @property roundValue Target value for rounding.
 * @property defaultComment Default log comment, used when none is specified in toggl.
 */
external interface Preferences {
    var jiraUrl: String
    var jiraServers: Array<Pair<String, String>> // Jira project keys csv -> Jira url
    var jiraUrls: Array<Pair<Int, String>> // Legacy: Toggl project id -> Jira url
    var mergeEntriesBy: String
    var jumpToToday: Boolean
    var togglApiToken: String
    var togglTemplate: String
    var roundType: String
    var roundValue: Int
    var defaultComment: String
}

@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
fun Preferences(prefs: Preferences.() -> Unit) = (js("{}") as Preferences).apply(prefs)
