package com.demich.cps.utils

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

object DmojApi {
    private val client = cpsHttpClient { }

    suspend fun getUser(handle: String): DmojUserResult {
        val str = client.getAs<String>(urls.main + "/api/v2/user/$handle")
        val json = Json.parseToJsonElement(str).jsonObject
        val obj = json["data"]!!.jsonObject["object"]!!
        return jsonCPS.decodeFromJsonElement(obj)
    }

    suspend fun getUserPage(handle: String): String {
        return client.getAs(urls.main + "/user/$handle")
    }

    suspend fun getSuggestions(query: String): List<DmojSuggestion> {
        val str = client.getAs<String>(urls.main + "/widgets/select2/user_search?term=$query&_type=query&q=$query")
        val json = Json.parseToJsonElement(str).jsonObject
        return jsonCPS.decodeFromJsonElement(json["results"]!!.jsonArray)
    }

    suspend fun getContests(): List<DmojContest> {
        val str = client.getAs<String>(urls.main + "/api/v2/contests")
        val json = Json.parseToJsonElement(str).jsonObject
        val obj = json["data"]!!.jsonObject["objects"]!!
        return jsonCPS.decodeFromJsonElement(obj)
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