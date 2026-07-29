package options

import Preferences
import api.TogglApi
import browser.permissions.contains
import browser.permissions.request
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.*
import kotlinx.coroutines.await
import kotlinx.html.dom.append
import kotlinx.html.js.*
import org.w3c.dom.*
import org.w3c.dom.events.Event
import utils.extensions.isVisible

@Suppress("UnsafeCastFromDynamic")
class Options {

    private val jiraUrl by lazy { document.getElementById("jira-url") as HTMLInputElement }
    private val mergeEntriesBy by lazy { document.getElementById("merge-entries-by") as HTMLSelectElement }
    private val togglApiToken by lazy { document.getElementById("toggl-api-token") as HTMLInputElement }
    private val togglTemplate by lazy { document.getElementById("template") as HTMLInputElement }
    private val templatePopup by lazy { document.getElementById("templatePopup") as HTMLElement }
    private val jumpToToday by lazy { document.getElementById("jump-to-today") as HTMLInputElement }
    private val roundType by lazy { document.getElementById("round-type") as HTMLSelectElement }
    private val roundValue by lazy { document.getElementById("round-value") as HTMLInputElement }
    private val roundValSection by lazy { document.getElementById("round-val-section") as HTMLElement }
    private val roundValLabel by lazy { document.getElementById("round-value-label") as HTMLElement }
    private val togglTokenButton by lazy { document.getElementById("toggl-token-button") as HTMLButtonElement }
    private val saveButton by lazy { document.getElementById("save") as HTMLButtonElement }
    private val addJiraServerButton get() = document.getElementById("btn-add-jira") as? HTMLButtonElement
    private val jiraSection by lazy { document.getElementById("jira-section") as HTMLElement }

    fun main() {
        GlobalScope.launch(Dispatchers.Main) {
            restoreOptions()
        }

        roundType.onchange = { onRoundChange() }
        saveButton.onclick = {
            onSaveClicked()
        }

        togglTokenButton.onclick = {
            togglTokenButton.firstElementChild?.classList?.toggle("loading")
            GlobalScope.launch {
                val status = document.getElementById("status")
                try {
                    TogglApi.getUserData(togglApiToken.value)
                    status?.textContent = "Toggl token OK."
                } catch (e: Exception) {
                    status?.textContent = "Toggl token check failed."
                }
                togglTokenButton.firstElementChild?.classList?.toggle("loading")
            }
        }

        addJiraServerButton?.onclick = { addJiraServerRow() }

        templatePopup.onclick = { e ->
            showPopup(e)
        }
    }

    private suspend fun restoreOptions() {
        val preferences = AppPreferences.getPreferences()
        jiraUrl.value = preferences.jiraUrl
        mergeEntriesBy.value = preferences.mergeEntriesBy
        jumpToToday.checked = preferences.jumpToToday
        togglApiToken.value = preferences.togglApiToken
        togglTemplate.value = preferences.togglTemplate
        roundType.value = preferences.roundType
        roundValue.value = preferences.roundValue.toString()
        roundValSection.isVisible = preferences.roundType != "no-round"
        preferences.jiraServers.forEach { server ->
            addJiraServerRow(keys = server.first, url = server.second)
        }
    }

    private fun onSaveClicked() {
        val urls = listOf(jiraUrl.value.trim()) + getAddedJiraServers().map { it.second }

        requestJiraPermission(urls) { granted ->
            if (granted) {
                GlobalScope.launch {
                    saveOptions()
                    window.alert("Options saved and permission granted.")
                }
            } else {
                window.alert("Permission denied. Options were not saved.")
            }
        }
    }


    private suspend fun saveOptions() {
        val options = Preferences {
            jiraUrl = this@Options.jiraUrl.value.trim().trimEnd('/')
            jiraServers = getAddedJiraServers().toTypedArray()
            jiraUrls = arrayOf() // legacy mapping is fully replaced by jiraServers once saved
            mergeEntriesBy = this@Options.mergeEntriesBy.value
            jumpToToday = this@Options.jumpToToday.checked
            togglApiToken = this@Options.togglApiToken.value
            togglTemplate = this@Options.togglTemplate.value
            roundType = this@Options.roundType.value
            roundValue = this@Options.roundValue.value.toInt()
        }

        AppPreferences.setPreferences(options).join()

        document.getElementById("status")?.apply {
            textContent = "Options saved."
            delay(750)
            textContent = ""
        }
    }

    private fun onRoundChange() =
        when (roundType.value) {
            "no-round" -> {
                roundValSection.isVisible = false
            }
            "round-up" -> {
                roundValSection.isVisible = true
                roundValLabel.textContent =
                    "Round duration to next x minutes. (15 will round to to the next quarter => 16 will become 30 etc.)"
            }
            "natural-round" -> {
                roundValSection.isVisible = true
                roundValLabel.textContent =
                    "Round duration to next x minutes. (15 will round to to the next quarter => 16 will become 30 etc.)"
            }
            "smart-round" -> {
                roundValSection.isVisible = true
                roundValLabel.textContent = "Target daily hours."
            }
            else -> {
            }
        }

    private fun addJiraServerRow(keys: String = "", url: String = "") {
        jiraSection.append {
            div(classes = "jira-server-row") {
                input(classes = "jira-url jira-server-url") {
                    placeholder = "https://other-jira.example.com"
                    value = url
                }
                input(classes = "jira-server-keys") {
                    placeholder = "ABC, INT"
                    value = keys
                }
                div(classes = "popup") {
                    onClickFunction = { e -> showPopup(e) }
                    i(classes = "far fa-question-circle")
                    span(classes = "popupText") {
                        text("Comma-separated Jira project keys logged to this server (e.g. ABC, INT). Issues with any other key go to the default server above.")
                    }
                }
            }
        }
    }

    private fun showPopup(e: Event) {
        (e.target as HTMLElement).parentElement?.querySelector(".popupText")?.classList?.toggle("show")
    }

    private fun getAddedJiraServers(): List<Pair<String, String>> =
        document.getElementsByClassName("jira-server-row").asList().mapNotNull { row ->
            val url = (row.querySelector(".jira-server-url") as HTMLInputElement).value.trim().trimEnd('/')
            val keys = (row.querySelector(".jira-server-keys") as HTMLInputElement).value.trim()
            if (url.isBlank()) null else keys to url
        }

    // Permissions for configured Jira urls

    private fun requestJiraPermission(urls: List<String>, callback: (Boolean) -> Unit) {
        val origins = urls
            .filter { it.isNotBlank() }
            .map { url -> if (url.endsWith("/")) "$url*" else "$url/*" }
            .distinct()
            .toTypedArray()
        val options = js("({})")
        options.origins = origins

        GlobalScope.launch {
            try {
                val alreadyGranted = contains(options).await()
                if (alreadyGranted) {
                    console.log("Already granted: ${origins.joinToString()}")
                    callback(true)
                } else {
                    val granted = request(options).await()
                    if (granted) {
                        console.log("Permission granted: ${origins.joinToString()}")
                    } else {
                        console.warn("Permission denied: ${origins.joinToString()}")
                    }
                    callback(granted)
                }
            } catch (e: dynamic) {
                console.warn("Permission check/request failed for ${origins.joinToString()}", e)
                callback(false)
            }
        }
    }
}
