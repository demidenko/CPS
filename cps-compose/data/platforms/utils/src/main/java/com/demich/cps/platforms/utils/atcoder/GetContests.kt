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
import kotlin.time.Duration
import kotlin.time.Instant

suspend fun AtCoderApi.getContests(): List<Contest> =
    AtCoderContestParser()
        .parseContests(source = getContestsPage())
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

    fun extractContest(timeElement: Element): Contest {
        val row = requireNotNull(timeElement.closest("tr"))
        val td = row.select("td")

        val title = td[1].expectFirst("a")
        val id = title.attr("href").removePrefix("/contests/")

        val timeString = timeElement.text()
        val startTime = Instant.parse(timeString, contestDateTimeFormat)

        val duration = Duration.parseHHMM(input = td[2].text())

        return Contest(
            platform = Contest.Platform.atcoder,
            title = title.text().trim(),
            id = id,
            link = AtCoderUrls.contest(id),
            startTime = startTime,
            duration = duration
        )
    }

    fun parseContests(source: String): Sequence<Contest> =
        Jsoup.parse(source).selectSequence("time.fixtime-full")
            .map(::extractContest)
}

internal fun Duration.Companion.parseHHMM(input: String): Duration {
    val i = input.indexOf(':')
    require(i != -1) { "expected ':' in $input" }
    require(input.length == i + 3) { "expected 2 minutes characters in $input" }
    val m = input[i+1].digitToInt() * 10 + input[i+2].digitToInt()
    val h = input.substring(0, i).toInt()
    return h.hours + m.minutes
}