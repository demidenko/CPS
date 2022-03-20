package com.demich.cps.utils

import io.ktor.client.request.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

object DmojAPI {
    private val client = cpsHttpClient { }

    suspend fun getUser(handle: String): DmojUserResult {
        val str = client.get<String>(URLFactory.main + "/api/v2/user/$handle")
        val json = Json.parseToJsonElement(str).jsonObject
        val obj = json["data"]!!.jsonObject["object"]!!
        return jsonCPS.decodeFromJsonElement(obj)
    }

    suspend fun getSuggestions(query: String): List<DmojSuggestion> {
        val str = client.get<String>(URLFactory.main + "/widgets/select2/user_search?term=$query&_type=query&q=$query")
        val json = Json.parseToJsonElement(str).jsonObject
        return jsonCPS.decodeFromJsonElement(
            json["results"]!!.jsonArray
        )
    }

    object URLFactory {
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
