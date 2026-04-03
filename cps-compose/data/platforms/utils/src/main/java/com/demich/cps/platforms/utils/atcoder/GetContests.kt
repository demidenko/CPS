package com.demich.cps.platforms.utils.atcoder

import com.demich.cps.contests.database.Contest
import com.demich.cps.platforms.api.atcoder.AtCoderApi
import com.demich.cps.platforms.api.atcoder.AtCoderUrls
import com.demich.cps.platforms.utils.selectSequence
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.char
import kotlinx.datetime.parse
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

suspend fun AtCoderApi.getContests(): List<Contest> =
    AtCoderContestParser()
        .extractContests(source = getContestsPage())
        .toList()

private class AtCoderContestParser {
    val contestDateTimeFormat = DateTimeComponents.Format {
        //YYYY-MM-DD hh:mm:ss+0900
        date(LocalDate.Formats.ISO)
        char(' ')
        time(LocalTime.Formats.ISO)
        offsetHours()
        offsetMinutesOfHour()
    }

    fun extractContestOrNull(timeElement: Element): Contest? {
        return kotlin.runCatching {
            val row = requireNotNull(timeElement.closest("tr"))
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

    fun extractContests(source: String): Sequence<Contest> =
        Jsoup.parse(source).selectSequence("time.fixtime-full")
            .mapNotNull(::extractContestOrNull)
}
