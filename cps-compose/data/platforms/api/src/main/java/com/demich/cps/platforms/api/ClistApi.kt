package com.demich.cps.platforms.api

import io.ktor.client.request.*
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

object ClistApi: PlatformApi {
    override val client = cpsHttpClient(json = defaultJson) { }

    suspend fun getUserPage(login: String): String {
        return client.getText(urls.user(login))
    }

    suspend fun getUsersSearchPage(str: String): String {
        return client.getText(urls.main + "/coders") {
            parameter("search", str)
        }
    }

    suspend fun getContests(
        apiAccess: ApiAccess,
        resourceIds: List<Int>,
        maxStartTime: Instant,
        minEndTime: Instant
    ): List<ClistContest> {
        if (resourceIds.isEmpty()) return emptyList()
        return client.getAs<ClistApiResponse<ClistContest>>("${urls.api}/contest") {
            parameter("format", "json")
            parameter("username", apiAccess.login)
            parameter("api_key", apiAccess.key)
            parameter("start__lte", maxStartTime.toString())
            parameter("end__gte", minEndTime.toString())
            parameter("resource_id__in", resourceIds.joinToString())
        }.objects
    }

    suspend fun getResources(
        apiAccess: ApiAccess
    ): List<ClistResource> {
        return client.getAs<ClistApiResponse<ClistResource>>(urlString = "${urls.api}/resource") {
            parameter("format", "json")
            parameter("username", apiAccess.login)
            parameter("api_key", apiAccess.key)
            parameter("limit", 1000)
        }.objects
    }

    object urls {
        const val main = "https://clist.by"
        fun user(login: String) = "$main/coder/$login"

        const val api = "$main/api/v3"
        val apiHelp get() = "$api/doc/"
    }

    @Serializable
    data class ApiAccess(
        val login: String,
        val key: String
    )
}

@Serializable
private class ClistApiResponse<T>(
    val objects: List<T>
)

@Serializable
data class ClistContest(
    val resource_id: Int,
    val id: Long,
    val start: String,
    val end: String,
    val event: String,
    val href: String,
    val host: String
)

@Serializable
data class ClistResource(
    val id: Int,
    val name: String
)