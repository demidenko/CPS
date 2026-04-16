package com.demich.cps.platforms.utils

import com.demich.cps.contests.database.ContestPlatform
import com.demich.cps.contests.database.toGeneralPlatform
import com.demich.cps.platforms.Platform
import com.demich.cps.platforms.api.clist.ClistContest
import com.demich.cps.platforms.api.clist.ClistResource
import com.demich.cps.profiles.userinfo.ClistUserInfo
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.char
import kotlinx.datetime.parse
import org.jsoup.Jsoup
import kotlin.time.Instant

object ClistUtils {
    private val contestDateFormat = DateTimeComponents.Format {
        //YYYY-MM-DDThh:mm:ss
        date(LocalDate.Formats.ISO)
        char('T')
        time(LocalTime.Formats.ISO)
    }

    fun parseContestDate(str: String): Instant =
        Instant.parse(str, contestDateFormat)

    fun makeResourceIds(
        platforms: Collection<Platform>,
        additionalResources: Collection<ClistResource>
    ): Set<Int> = buildSet {
        platforms.forEach { add(requireNotNull(it.clistResourceId)) }
        additionalResources.forEach { add(it.id) }
    }

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
            login = login,
            accounts = accounts
        )
    }
}

fun ClistContest.extractContestId(platform: ContestPlatform?): String =
    when (platform) {
        codeforces -> {
            href.removePrefixHttp().removePrefix("codeforces.com/contests/")
                .toIntOrNull()?.toString()
        }
        atcoder -> {
            href.removePrefixHttp().removePrefix("atcoder.jp/contests/")
        }
        codechef -> {
            href.removePrefixHttp().removePrefix("www.codechef.com/")
        }
        dmoj -> {
            href.removePrefixHttp().removePrefix("dmoj.ca/contest/")
        }
        else -> null
    } ?: id.toString()

private fun String.removePrefixHttp() =
    removePrefix("http://").removePrefix("https://")

fun ContestPlatform.clistResourceId(): Int =
    requireNotNull(toGeneralPlatform().clistResourceId)

val Platform.clistResourceId: Int?
    get() = when (this) {
        codeforces -> 1
        codechef -> 2
        acmp -> 5
        timus -> 7
        topcoder -> 12
        project_euler -> 65
        dmoj -> 77
        atcoder -> 93
        leetcode -> 102
        clist -> null
    }