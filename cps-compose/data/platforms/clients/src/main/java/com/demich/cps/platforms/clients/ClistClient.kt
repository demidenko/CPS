package com.demich.cps.platforms.clients

import com.demich.cps.platforms.api.clist.ClistApi
import com.demich.cps.platforms.api.clist.ClistApiAccess
import com.demich.cps.platforms.api.clist.ClistContest
import com.demich.cps.platforms.api.clist.ClistPageContentProvider
import com.demich.cps.platforms.api.clist.ClistResource
import com.demich.cps.platforms.api.clist.ClistUrls
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.parameter
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class ClistClient(
    val apiAccess: ClistApiAccess? = null
): ClistApi, ClistPageContentProvider {

    private suspend inline fun getPageText(
        url: String,
        block: HttpRequestBuilder.() -> Unit = {}
    ): String {
        return clistHttpClient.getText(urlString = url, block = block)
    }

    override suspend fun getUserPage(login: String): String {
        return getPageText(url = ClistUrls.user(login))
    }

    override suspend fun getUsersSearchPage(str: String): String {
        return getPageText(url = "coders") {
            parameter("search", str)
        }
    }

    private suspend inline fun <reified T> getApi(
        method: String,
        block: HttpRequestBuilder.() -> Unit = {}
    ): T {
        return clistHttpClient.getAs(urlString = "${ClistUrls.api}/$method") {
            block()
            parameter("format", "json")
            if (apiAccess != null) {
                parameter("username", apiAccess.login)
                parameter("api_key", apiAccess.key)
            }
        }
    }

    private suspend inline fun <reified T> getApiJsonObjects(
        method: String,
        responseSizeLimit: Int = 1000,
        block: HttpRequestBuilder.() -> Unit = {}
    ): List<T> = buildList {
        //TODO: what if meta.total_count changes?
        var offset = 0
        do {
            val result = getApi<ClistApiResponse<T>>(method = method) {
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
        resourceIds: Collection<Int>,
        maxStartTime: Instant,
        minEndTime: Instant
    ): List<ClistContest> {
        if (resourceIds.isEmpty()) return emptyList()
        return getApiJsonObjects(method = "contest") {
            parameter("start__lte", maxStartTime)
            parameter("end__gte", minEndTime)
            parameter("resource_id__in", resourceIds.joinToString(separator = ","))
        }
    }

    override suspend fun getResources(): List<ClistResource> {
        return getApiJsonObjects(method = "resource")
    }
}

private val clistHttpClient = cpsHttpClient(json = defaultJson) {
    defaultRequest {
        url(ClistUrls.main)
    }

    install(RateLimitPlugin) {
        10 per 1.minutes // https://clist.by/api/v4/doc/ #Throttle
        2 per 1.seconds
        if (BuildConfig.DEBUG) {
            1 per 3.seconds
        }
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
