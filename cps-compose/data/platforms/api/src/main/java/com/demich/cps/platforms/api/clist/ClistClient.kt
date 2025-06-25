package com.demich.cps.platforms.api.clist

import com.demich.cps.platforms.api.PlatformClient
import com.demich.cps.platforms.api.RateLimitPlugin
import com.demich.cps.platforms.api.cpsHttpClient
import com.demich.cps.platforms.api.defaultJson
import com.demich.cps.platforms.api.getAs
import com.demich.cps.platforms.api.getText
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.parameter
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object ClistClient: PlatformClient, ClistApi, ClistPageContentProvider {
    override val client = cpsHttpClient(json = defaultJson) {
        defaultRequest {
            url(ClistUrls.main)
        }

        install(RateLimitPlugin) {
            10 per 1.minutes // https://clist.by/api/v4/doc/ #Throttle
            2 per 1.seconds
        }
    }

    override suspend fun getUserPage(login: String): String {
        return client.getText(ClistUrls.user(login))
    }

    override suspend fun getUsersSearchPage(str: String): String {
        return client.getText("coders") {
            parameter("search", str)
        }
    }

    private suspend inline fun <reified T> getApiJsonObjects(
        page: String,
        apiAccess: ClistApi.ApiAccess,
        responseSizeLimit: Int = 1000,
        block: HttpRequestBuilder.() -> Unit = {}
    ): List<T> = buildList {
        //TODO: what if meta.total_count changes?
        var offset = 0
        do {
            val result = client.getAs<ClistApiResponse<T>>("${ClistUrls.api}/$page") {
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

    override suspend fun getContests(
        apiAccess: ClistApi.ApiAccess,
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

    override suspend fun getResources(apiAccess: ClistApi.ApiAccess): List<ClistResource> {
        return getApiJsonObjects(page = "resource", apiAccess = apiAccess)
    }
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
