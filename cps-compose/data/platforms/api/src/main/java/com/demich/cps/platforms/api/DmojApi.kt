package com.demich.cps.platforms.api

import com.demich.kotlin_stdlib_boost.ifBetweenFirstFirst
import com.demich.kotlin_stdlib_boost.ifBetweenFirstLast
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.parameter
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.time.Duration.Companion.seconds

object DmojApi: PlatformApi {
    private val json get() = defaultJson
    override val client = cpsHttpClient(json = json) {
        defaultRequest {
            url(urls.main)
        }

        // https://docs.dmoj.ca/#/site/api?id=rate-limiting "90 requests per minute"
        install(RateLimitPlugin) {
            3 per 2.seconds
        }
    }

    private inline fun <reified T> JsonObject.getObject(key: String): T =
        json.decodeFromJsonElement(getValue(key))

    private suspend inline fun getApiDataObject(
        path: String
    ): JsonObject {
        return client.getAs<JsonObject>(urlString = "/api/v2/$path")
            .getValue("data").jsonObject
    }

    suspend fun getUserPage(handle: String): String {
        return client.getText(urlString = "/user/$handle")
    }

    suspend fun getUser(handle: String): DmojUserResult {
        return getApiDataObject(path = "user/$handle").getObject(key = "object")
    }

    suspend fun getSuggestions(str: String): List<DmojSuggestion> {
        return client.getAs<JsonObject>(urlString = "/widgets/select2/user_search") {
            parameter("_type", "query")
            parameter("term", str)
            parameter("q", str)
        }.getObject(key = "results")
    }

    suspend fun getContests(): List<DmojContest> {
        return getApiDataObject(path = "contests").getObject(key = "objects")
    }

    suspend fun getRatingChanges(handle: String): List<DmojRatingChange> {
        ifBetweenFirstFirst(
            str = getUserPage(handle = handle),
            from = "var rating_history",
            to = ";"
        ) { str ->
            ifBetweenFirstLast(str, from = "[", to = "]", include = true) {
                return json.decodeFromString(it)
            }
        }
        return emptyList()
    }

    object urls {
        const val main = "https://dmoj.ca"
        fun user(username: String) = "$main/user/$username"
    }
}

@Serializable
data class DmojUserResult(
    val id: Int,
    val username: String,
    val rating: Int?
)

@Serializable
data class DmojSuggestion(
    val text: String,
    val id: String
)

@Serializable
data class DmojRatingChange(
    val label: String,
    val rating: Int,
    val ranking: Int,
    val link: String,
    val timestamp: Double
)

@Serializable
data class DmojContest(
    val key: String,
    val name: String,

    //contest start time in ISO format
    val start_time: String,

    //contest end time in ISO format
    val end_time: String,

    //contest time limit in seconds, or null if the contest is not windowed
    //Double because of "time_limit":10800.0
    val time_limit: Double?
)