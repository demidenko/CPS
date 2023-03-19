package com.demich.cps.platforms.utils

import com.demich.cps.accounts.userinfo.AtCoderUserInfo
import com.demich.cps.accounts.userinfo.STATUS
import com.demich.cps.contests.database.Contest
import com.demich.cps.platforms.api.AtCoderApi
import kotlinx.datetime.Instant
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

object AtCoderUtils {
    fun extractUserInfo(source: String): AtCoderUserInfo =
        with(Jsoup.parse(source)) {
            AtCoderUserInfo(
                status = STATUS.OK,
                handle = expectFirst("a.username").text(),
                rating = select("th.no-break").find { it.text() == "Rating" }
                    ?.nextElementSibling()
                    ?.text()?.toInt()
            )
        }

    private fun extractContestOrNull(timeElement: Element): Contest? {
        return kotlin.runCatching {
            val row = timeElement.parents().find { it.normalName() == "tr" }!!
            val td = row.select("td")

            //YYYY-MM-DD hh:mm:ss+0900
            val timeString = timeElement.text()
            val startTime = Instant.parse(timeString.replace("+0900", "+09:00").replace(' ', 'T'))

            val duration = td[2].text().split(':').let {
                val h = it[0].toInt()
                val m = it[1].toInt()
                h.hours + m.minutes
            }

            val title = td[1].expectFirst("a")
            val id = title.attr("href").removePrefix("/contests/")

            Contest(
                platform = Contest.Platform.atcoder,
                title = title.text().trim(),
                id = id,
                link = AtCoderApi.urls.contest(id),
                startTime = startTime,
                durationSeconds = duration.inWholeSeconds
            )
        }.getOrNull()
    }

    fun extractContests(source: String): List<Contest> =
        Jsoup.parse(source).select("time.fixtime-full")
            .mapNotNull(::extractContestOrNull)
}