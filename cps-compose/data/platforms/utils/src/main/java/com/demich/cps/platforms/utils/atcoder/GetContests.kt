package com.demich.cps.platforms.utils.atcoder

import com.demich.cps.contests.database.Contest
import com.demich.cps.platforms.api.atcoder.AtCoderApi
import com.demich.cps.platforms.api.atcoder.AtCoderUrls
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.format.char
import kotlinx.datetime.parse
import kotlinx.datetime.toDeprecatedInstant
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

suspend fun AtCoderApi.getContests(): List<Contest> {
    val source = getContestsPage()

    val contestDateTimeFormat = DateTimeComponents.Format {
        //YYYY-MM-DD hh:mm:ss+0900
        date(LocalDate.Formats.ISO)
        char(' ')
        time(LocalTime.Formats.ISO)
        offsetHours()
        offsetMinutesOfHour()
    }

    return Jsoup.parse(source).select("time.fixtime-full")
        .mapNotNull {
            extractContestOrNull(it, contestDateTimeFormat)
        }
}

private fun extractContestOrNull(
    timeElement: Element,
    format: DateTimeFormat<DateTimeComponents>
): Contest? {
    return runCatching {
        val row = timeElement.parents().first { it.normalName() == "tr" }
        val td = row.select("td")

        val timeString = timeElement.text()
        val startTime = Instant.parse(timeString, format)

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
            startTime = startTime.toDeprecatedInstant(),
            duration = duration
        )
    }.getOrNull()
}
