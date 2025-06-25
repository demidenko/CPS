package com.demich.cps.platforms.utils

import com.demich.cps.accounts.userinfo.AtCoderUserInfo
import com.demich.cps.accounts.userinfo.UserSuggestion
import com.demich.cps.contests.database.Contest
import com.demich.cps.platforms.api.atcoder.AtCoderUrls
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.char
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

object AtCoderUtils {
    fun extractUserInfo(source: String): AtCoderUserInfo =
        with(Jsoup.parse(source)) {
            AtCoderUserInfo(
                handle = expectFirst("a.username").text(),
                rating = select("th.no-break").find { it.text() == "Rating" }
                    ?.nextElementSibling()
                    ?.selectFirst("span")
                    ?.text()?.toInt()
            )
        }

    private val contestDateTimeFormat by lazy {
        DateTimeComponents.Format {
            //YYYY-MM-DD hh:mm:ss+0900
            date(LocalDate.Formats.ISO)
            char(' ')
            time(LocalTime.Formats.ISO)
            offsetHours()
            offsetMinutesOfHour()
        }
    }

    private fun extractContestOrNull(timeElement: Element): Contest? {
        return kotlin.runCatching {
            val row = timeElement.parents().first { it.normalName() == "tr" }
            val td = row.select("td")

            val timeString = timeElement.text()
            val startTime = Instant.parse(timeString, contestDateTimeFormat)

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
                link = AtCoderUrls.contest(id),
                startTime = startTime,
                duration = duration
            )
        }.getOrNull()
    }

    fun extractContests(source: String): List<Contest> =
        Jsoup.parse(source).select("time.fixtime-full")
            .mapNotNull(::extractContestOrNull)

    fun extractUserSuggestions(source: String): List<UserSuggestion> {
        val table = Jsoup.parse(source).expectFirst("table.table")
        val ratingIndex = table.select("thead > tr > th").indexOfFirst { it.text() == "Rating" }
        return table.select("tbody > tr").map { row ->
            UserSuggestion(
                userId = row.expectFirst("a.username").text(),
                info = row.select("td")[ratingIndex].text()
            )
        }
    }

    class NewsPost(
        val title: String,
        val time: Instant?,
        override val id: String
    ): NewsPostEntry


    fun extractNews(source: String): List<NewsPost?> =
        Jsoup.parse(source).select("div.panel.panel-default, div.panel.panel-info")
            .mapNotNull { panel ->
                val header = panel.expectFirst("div.panel-heading")
                val titleElement = header.expectFirst("h3.panel-title")
                val timeElement = header.selectFirst("span.tooltip-unix") ?: return@mapNotNull null
                val id = titleElement.expectFirst("a").attr("href").removePrefix("/posts/")
                NewsPost(
                    title = titleElement.text(),
                    time = timeElement.let {
                        Instant.fromEpochSeconds(it.attr("title").toLong())
                    },
                    id = id
                )
            }
            .sortedByDescending { it.id }
}