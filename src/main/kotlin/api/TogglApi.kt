package api

import api.models.TogglTimeEntry
import api.models.TogglTimeEntryList
import api.models.TogglUserData
import io.ktor.client.HttpClient
import io.ktor.client.call.*
import io.ktor.client.engine.js.Js
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import utils.extensions.toBase64
import kotlin.js.Date

/**
 * Toggl related endpoints.
 */
object TogglApi {

    private lateinit var client: HttpClient
    private lateinit var apiToken: String

    /**
     * Initializes HttpClient with base URL.
     */
    fun init(apiToken: String) {
        this.apiToken = apiToken
        client = HttpClient(Js) {
            defaultRequest {
                host = "api.track.toggl.com"
                url.protocol = URLProtocol.HTTPS
                header("Authorization", "Basic " + "${this@TogglApi.apiToken}:api_token".toBase64())
            }
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
            HttpResponseValidator {
                validateResponse { response ->
                    when (response.status.value) {
                        in 300..399 -> throw RedirectResponseException(response, response.toString())
                        in 400..499 -> throw ClientRequestException(response, response.toString())
                        in 500..599 -> throw ServerResponseException(response, response.toString())
                    }
                    if (response.status.value >= 600) {
                        throw ResponseException(response, response.toString())
                    }
                }
            }
        }
    }

    /**
     * Get time entries for specified date range.
     *
     * @param startDate Start date in ISO format.
     * @param endDate End date in ISO format.
     */
    suspend fun getTimeEntries(startDate: Date, endDate: Date): List<TogglTimeEntry> =
        client.get {
            url {
                encodedPath = "/api/v9/me/time_entries?start_date=${startDate.toISOString()}&end_date=${endDate.toISOString()}"
            }
        }.body<TogglTimeEntryList>().items

    /**
     * Get toggl user data.
     */
    suspend fun getUserData(token: String): TogglUserData {
        apiToken = token
        return client.get {
            url {
                encodedPath = "/api/v9/me?with_related_data=true"
            }
        }.body()
    }
}