package com.demich.cps.platforms.utils

import com.demich.cps.accounts.userinfo.ClistUserInfo
import com.demich.cps.accounts.userinfo.STATUS
import com.demich.cps.contests.database.Contest
import com.demich.cps.platforms.api.ClistContest
import com.demich.cps.platforms.api.ClistResource
import org.jsoup.Jsoup
import kotlin.collections.set

object ClistUtils {
    suspend fun makeResourceIds(
        platforms: Collection<Contest.Platform>,
        additionalResources: suspend () -> Collection<ClistResource> = { emptyList() }
    ): List<Int> = buildList {
        for (platform in platforms) {
            if (platform == Contest.Platform.unknown) addAll(additionalResources().map { it.id })
            else add(getClistApiResourceId(platform))
        }
    }

    fun getClistApiResourceId(platform: Contest.Platform): Int =
        when (platform) {
            Contest.Platform.unknown -> throw IllegalArgumentException("unknown not allowed")
            Contest.Platform.codeforces -> 1
            Contest.Platform.atcoder -> 93
            Contest.Platform.topcoder -> 12
            Contest.Platform.codechef -> 2
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

    fun extractUserInfo(source: String, login: String): ClistUserInfo {
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
            val link = button.getElementsByClass("btn-xs").first()?.attr("href") ?: ""
            val span = a[1].selectFirst("span") ?: return@forEach
            val userName = span.run {
                val attr = "title"
                val withAttr = getElementsByAttribute(attr).first()
                if (withAttr == null) text()
                else withAttr.attr(attr)
            }
            accounts[resource] = userName to link
        }

        return ClistUserInfo(
            status = STATUS.OK,
            login = login,
            accounts = accounts
        )
    }
}

private fun String.removePrefixHttp() = removePrefix("http://").removePrefix("https://")
