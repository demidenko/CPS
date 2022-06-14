package com.demich.cps.utils

import com.demich.cps.accounts.managers.RatingChange
import io.ktor.client.request.*
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

object DmojApi {
    private val client = cpsHttpClient { }

    suspend fun getUser(handle: String): DmojUserResult {
        val str = client.getText(urlString = "${urls.main}/api/v2/user/$handle")
        val json = Json.parseToJsonElement(str).jsonObject
        val obj = json["data"]!!.jsonObject["object"]!!
        return jsonCPS.decodeFromJsonElement(obj)
    }

    suspend fun getUserPage(handle: String): String {
        return client.getText(urlString = "${urls.main}/user/$handle")
    }

    suspend fun getSuggestions(str: String): List<DmojSuggestion> {
        val src = client.getText(urlString = "${urls.main}/widgets/select2/user_search") {
            parameter("_type", "query")
            parameter("term", str)
            parameter("q", str)
        }
        val json = Json.parseToJsonElement(src).jsonObject
        return jsonCPS.decodeFromJsonElement(json["results"]!!.jsonArray)
    }

    suspend fun getContests(): List<DmojContest> {
        val str = client.getText(urlString = "${urls.main}/api/v2/contests")
        val json = Json.parseToJsonElement(str).jsonObject
        val obj = json["data"]!!.jsonObject["objects"]!!
        return jsonCPS.decodeFromJsonElement(obj)
    }

    suspend fun getRatingChanges(handle: String): List<DmojRatingChange> {
        val s = getUserPage(handle = handle)
        val i = s.indexOf("var rating_history = [")
        if (i == -1) return emptyList()
        val j = s.indexOf("];", i)
        val str = s.substring(s.indexOf('[', i), j+1)
        return jsonCPS.decodeFromString(str)
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
) {
    fun toRatingChange() =
        RatingChange(
            rating = rating,
            date = Instant.fromEpochMilliseconds(timestamp.toLong()),
            title = label,
            rank = ranking
        )
}

@Serializable
data class DmojContest(
    val key: String,
    val name: String,
    val start_time: String,
    val end_time: String
)