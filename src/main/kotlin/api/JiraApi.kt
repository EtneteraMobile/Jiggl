package api

import api.models.JiraUser
import api.models.LogWorkInput
import api.models.JiraWorklog
import io.ktor.client.HttpClient
import io.ktor.client.call.*
import io.ktor.client.engine.js.Js
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.browser.window
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.await
import browser.tabs.query
import browser.tabs.sendMessage
import org.w3c.fetch.Headers
import org.w3c.fetch.RequestInit

/**
 * JIRA related endpoints.
 *
 * https://docs.atlassian.com/software/jira/docs/api/REST/8.5.4/
 */
object JiraApi {

    private lateinit var client: HttpClient

    /**
     * Initializes HttpClient with base URL.
     */
    fun init(baseUrl: String) {
        Url(baseUrl).let {
            client = HttpClient(Js) {
                defaultRequest {
                    url.protocol = it.protocol
                    url.encodedPath = it.encodedPath
                    // Firefox sends an Origin header for extension requests which triggers
                    // Jira's XSRF protection on modifying requests. Adding this header
                    // allows cross-origin POST/PUT from the extension while authenticated.
                    header("X-Atlassian-Token", "no-check")
//                    header("Access-Control-Allow-Origin", "*")
                }
                install(ContentNegotiation) {
                    json(Json {
                        ignoreUnknownKeys = true
                    })
                }
                addDefaultResponseValidation()
            }
        }
    }

    /**
     * Returns JIRA user info.
     */
    suspend fun getUserData(serverHost: String): JiraUser =
        client.get {
            url {
                protocol = Url(serverHost).protocol
                host = Url(serverHost).host
                encodedPath = "/rest/api/2/myself"
            }
        }.body()

    /**
     * Logs work to Jira.
     */
    suspend fun logWork(serverHost: String, issue: String, log: LogWorkInput): HttpStatusCode {
        val base = Url(serverHost)
        val pattern = "${base.protocol.name}://${base.host}/*"
        val body = Json.encodeToString(LogWorkInput.serializer(), log)

        // Try to use a content script on an open Jira tab for same-origin POST
        val qi = js("({})")
        qi.url = arrayOf(pattern)
        val tabs = try {
            query(qi).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: dynamic) {
            emptyArray()
        }

        val availableTabs = tabs.filter { it.id != null && it.discarded != true && it.frozen != true }
        val tabId = (availableTabs.firstOrNull { it.active == true } ?: availableTabs.firstOrNull())?.id
        if (tabId != null) {
            val msg = js("({})")
            msg.type = "jiggl/logWork"
            msg.issue = issue
            msg.body = body
            try {
                val reply = sendMessage(tabId, msg).await()
                val status = reply?.status as? Int
                if (status != null && status > 0) return HttpStatusCode.fromValue(status)
                error(reply?.error as? String ?: "The Jira tab did not confirm the request.")
            } catch (e: dynamic) {
                // A missing content script cannot have issued a POST. Other failures may occur after delivery.
                val message = e?.message as? String
                if (message?.contains("Receiving end does not exist") != true) throw e
            }
        }

        // Fallback: direct cross-origin fetch (works in Chrome; may 403 in Firefox)
        val url = "${base.protocol.name}://${base.host}/rest/api/latest/issue/$issue/worklog"
        val headers = Headers()
        headers.append("Content-Type", "application/json")
        headers.append("X-Atlassian-Token", "no-check")
        val init = RequestInit(method = "POST", headers = headers, body = body)
        init.asDynamic().credentials = "include"
        val resp = window.fetch(url, init).await()
        return HttpStatusCode.fromValue(resp.status.toInt())
    }

    /**
     * Gets worklog for given Jira task.
     */
    suspend fun getWorklog(serverHost: String, issue: String): JiraWorklog =
        client.get {
            url {
                protocol = Url(serverHost).protocol
                host = Url(serverHost).host
                encodedPath = "/rest/api/latest/issue/$issue/worklog"
            }
        }.body()
}
