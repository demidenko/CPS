package com.demich.cps.platforms.api

import io.ktor.client.request.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*

object DmojApi: PlatformApi {
    private val json get() = defaultJson
    override val client = cpsHttpClient(json = json) { }

    suspend fun getUser(handle: String): DmojUserResult {
        val obj = client.getAs<JsonObject>(urlString = "${urls.main}/api/v2/user/$handle")
            .getValue("data").jsonObject
            .getValue("object")
        return json.decodeFromJsonElement(obj)
    }

    suspend fun getUserPage(handle: String): String {
        return client.getText(urlString = "${urls.main}/user/$handle")
    }

    suspend fun getSuggestions(str: String): List<DmojSuggestion> {
        val obj = client.getAs<JsonObject>(urlString = "${urls.main}/widgets/select2/user_search") {
            parameter("_type", "query")
            parameter("term", str)
            parameter("q", str)
        }
        return json.decodeFromJsonElement(obj.getValue("results"))
    }

    suspend fun getContests(): List<DmojContest> {
        val obj = client.getAs<JsonObject>(urlString = "${urls.main}/api/v2/contests")
            .getValue("data").jsonObject
            .getValue("objects")
        return json.decodeFromJsonElement(obj)
    }

    suspend fun getRatingChanges(handle: String): List<DmojRatingChange> {
        val s = getUserPage(handle = handle)
        val i = s.indexOf("var rating_history = [")
        if (i == -1) return emptyList()
        val j = s.indexOf("];", i)
        val str = s.substring(s.indexOf('[', i), j+1)
        return json.decodeFromString(str)
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
    val start_time: String,
    val end_time: String
)