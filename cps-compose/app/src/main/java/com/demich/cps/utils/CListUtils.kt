package com.demich.cps.utils

import com.demich.cps.accounts.managers.AccountManagers
import com.demich.cps.accounts.managers.CListUserInfo
import com.demich.cps.accounts.managers.STATUS
import com.demich.cps.contests.Contest
import io.ktor.client.request.*
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import org.jsoup.Jsoup

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

    fun extractLoginSuggestions(source: String): List<String> =
        Jsoup.parse(source).select("td.username").map { it.text() }

    fun extractUserInfo(source: String, login: String): CListUserInfo {
        val accounts = mutableMapOf<String, Pair<String, String>>()

        val body = Jsoup.parse(source).body()
        //rated table
        body.getElementById("table-accounts")
            ?.select("tr")
            ?.forEach { row ->
                val cols = row.select("td")
                val href = cols[0].getElementsByAttribute("href").attr("href")
                val resource = href.removePrefix("/resource/").removeSuffix("/")
                val link = cols[3].getElementsByAttribute("href").attr("href")
                val userName = cols[2].select("span").map { it.text() }.firstOrNull { it.isNotBlank() } ?: return@forEach
                accounts[resource] = userName to link
            }

        //buttons
        body.getElementsByClass("account btn-group btn-group-sm").forEach { button ->
            val a = button.select("a")
            if (a.isEmpty()) return@forEach
            val href = a[0].attr("href")
            val resource = href.removePrefix("/resource/").removeSuffix("/")
            val link = button.getElementsByClass("fas fa-external-link-alt").first()?.parent()?.attr("href") ?: ""
            val span = a[1].selectFirst("span") ?: return@forEach
            val userName = span.run {
                val attr = "title"
                val withAttr = getElementsByAttribute(attr).first()
                if (withAttr == null) text()
                else withAttr.attr(attr)
            }
            accounts[resource] = userName to link
        }

        return CListUserInfo(
            status = STATUS.OK,
            login = login,
            accounts = accounts
        )
    }
}

object CListApi: ResourceApi {
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
