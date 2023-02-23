package com.demich.cps.utils

import com.demich.cps.accounts.managers.AccountManagers
import com.demich.cps.contests.Contest
import io.ktor.client.request.*
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

object CListUtils {
    fun getManager(resource: String, userName: String, link: String): Pair<AccountManagers, String>? {
        return when (resource) {
            "codeforces.com" -> AccountManagers.codeforces to userName
            "atcoder.jp" -> AccountManagers.atcoder to userName
            "codechef.com" -> AccountManagers.codechef to userName
            "dmoj.ca" -> AccountManagers.dmoj to userName
            "acm.timus.ru", "timus.online" -> {
                val userId = link.substring(link.lastIndexOf('=')+1)
                AccountManagers.timus to userId
            }
            else -> null
        }
    }

    fun getClistApiResourceId(platform: Contest.Platform): Int =
        when (platform) {
            Contest.Platform.unknown -> throw IllegalArgumentException("unknown not allowed")
            Contest.Platform.codeforces -> 1
            Contest.Platform.atcoder -> 93
            Contest.Platform.topcoder -> 12
            Contest.Platform.codechef -> 2
            Contest.Platform.google -> 35
            Contest.Platform.dmoj -> 77
        }

    fun extractContestId(contest: ClistContest, platform: Contest.Platform?): String =
        when (platform) {
            Contest.Platform.codeforces -> {
                contest.href.removePrefixHttp().removePrefix("codeforces.com/contests/")
                    .toIntOrNull()?.toString()
            }
            Contest.Platform.atcoder -> {
                contest.href.removePrefixHttp().removePrefix("atcoder.jp/contests/")
            }
            Contest.Platform.codechef -> {
                contest.href.removePrefixHttp().removePrefix("www.codechef.com/")
            }
            Contest.Platform.dmoj -> {
                contest.href.removePrefixHttp().removePrefix("dmoj.ca/contest/")
            }
            else -> null
        } ?: contest.id.toString()
}

object CListApi: ResourceApi() {
    override val client = cpsHttpClient(json = json) { }

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
        platforms: Collection<Contest.Platform>,
        includeResourceIds: suspend () -> Collection<Int> = { emptyList() },
        maxStartTime: Instant,
        minEndTime: Instant
    ): List<ClistContest> {
        val resourceIds: List<Int> = buildList {
            for (platform in platforms) {
                if (platform == Contest.Platform.unknown) addAll(includeResourceIds())
                else add(CListUtils.getClistApiResourceId(platform))
            }
        }
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

        const val api = "$main/api/v2"
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

private fun String.removePrefixHttp() = removePrefix("http://").removePrefix("https://")
