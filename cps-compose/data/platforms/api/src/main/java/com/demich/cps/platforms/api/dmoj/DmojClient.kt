package com.demich.cps.platforms.api.dmoj

import com.demich.cps.platforms.api.PlatformClient
import com.demich.cps.platforms.api.RateLimitPlugin
import com.demich.cps.platforms.api.cpsHttpClient
import com.demich.cps.platforms.api.defaultJson
import com.demich.cps.platforms.api.getAs
import com.demich.cps.platforms.api.getText
import com.demich.kotlin_stdlib_boost.ifBetweenFirstFirst
import com.demich.kotlin_stdlib_boost.ifBetweenFirstLast
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.parameter
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.time.Duration.Companion.seconds

object DmojClient: PlatformClient, DmojApi, DmojPageContentProvider {
    private val json get() = defaultJson
    override val client = cpsHttpClient(json = json) {
        defaultRequest {
            url(DmojUrls.main)
        }

        install(RateLimitPlugin) {
            3 per 2.seconds // https://docs.dmoj.ca/#/site/api?id=rate-limiting "90 requests per minute"
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

    override suspend fun getUserPage(handle: String): String {
        return client.getText(urlString = "/user/$handle")
    }

    override suspend fun getUser(handle: String): DmojUserResult {
        return getApiDataObject(path = "user/$handle").getObject(key = "object")
    }

    override suspend fun getSuggestions(str: String): List<DmojSuggestion> {
        return client.getAs<JsonObject>(urlString = "/widgets/select2/user_search") {
            parameter("_type", "query")
            parameter("term", str)
            parameter("q", str)
        }.getObject(key = "results")
    }

    override suspend fun getContests(): List<DmojContest> {
        return getApiDataObject(path = "contests").getObject(key = "objects")
    }

    override suspend fun getRatingChanges(handle: String): List<DmojRatingChange> {
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
}

