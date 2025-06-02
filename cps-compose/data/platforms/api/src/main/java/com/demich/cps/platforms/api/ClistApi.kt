package com.demich.cps.platforms.api

import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.parameter
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.minutes

object ClistApi: PlatformApi {
    override val client = cpsHttpClient(json = defaultJson) {
        defaultRequest {
            url(urls.main)
        }

        install(RateLimitPlugin) {
            // from https://clist.by/api/v4/doc/ #Throttle
            window = 1.minutes
            requestsPerWindow = 10
        }
    }

    suspend fun getUserPage(login: String): String {
        return client.getText(urls.user(login))
    }

    suspend fun getUsersSearchPage(str: String): String {
        return client.getText("coders") {
            parameter("search", str)
        }
    }

    private suspend inline fun<reified T> getApiJsonObjects(
        page: String,
        apiAccess: ApiAccess,
        responseSizeLimit: Int = 1000,
        block: HttpRequestBuilder.() -> Unit = {}
    ): List<T> = buildList {
        //TODO: what if meta.total_count changes?
        var offset = 0
        do {
            val result = client.getAs<ClistApiResponse<T>>("${urls.api}/$page") {
                parameter("format", "json")
                parameter("username", apiAccess.login)
                parameter("api_key", apiAccess.key)
                parameter("limit", responseSizeLimit)
                parameter("offset", offset)
                parameter("total_count", true)
                block()
            }
            result.objects.let {
                addAll(it)
                offset += it.size
            }
            val totalCount = requireNotNull(result.meta.total_count)
        } while (offset < totalCount)
    }

    suspend fun getContests(
        apiAccess: ApiAccess,
        resourceIds: List<Int>,
        maxStartTime: Instant,
        minEndTime: Instant
    ): List<ClistContest> {
        if (resourceIds.isEmpty()) return emptyList()
        return getApiJsonObjects(page = "contest", apiAccess = apiAccess) {
            parameter("start__lte", maxStartTime.toString())
            parameter("end__gte", minEndTime.toString())
            parameter("resource_id__in", resourceIds.joinToString())
        }
    }

    suspend fun getResources(apiAccess: ApiAccess): List<ClistResource> {
        return getApiJsonObjects(page = "resource", apiAccess = apiAccess)
    }

    object urls {
        const val main = "https://clist.by"
        fun user(login: String) = "$main/coder/$login"

        const val api = "$main/api/v4"
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
    val objects: List<T>,
    val meta: PageInfo
)

@Serializable
private class PageInfo(
    //val limit: Int,
    //val next: String,
    //val offset: Int,
    //val previous: String?,
    val total_count: Int?
)

@Serializable
data class ClistContest(
    val resource_id: Int,
    val id: Long,
    val start: String,
    val end: String,
    val duration: Long,
    val event: String,
    val href: String,
    val host: String
)

@Serializable
data class ClistResource(
    val id: Int,
    val name: String
)