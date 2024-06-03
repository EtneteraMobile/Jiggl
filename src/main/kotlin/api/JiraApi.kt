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
//                    header("X-Atlassian-Token", "nocheck") // may be needed, not sure yet
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
        val response = client.post {
            url {
                protocol = Url(serverHost).protocol
                host = Url(serverHost).host
                encodedPath = "/rest/api/latest/issue/$issue/worklog"
            }
            contentType(ContentType.Application.Json)
            setBody(log)
        }
        return response.status
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
