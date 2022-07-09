package com.demich.cps.contests.loaders

import com.demich.cps.contests.Contest
import com.demich.cps.contests.settings.ContestDateConstraints
import com.demich.cps.utils.AtCoderApi
import kotlinx.datetime.Instant
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class AtCoderContestsLoader: ContestsLoader(type = ContestsLoaders.atcoder) {
    override suspend fun loadContests(
        platform: Contest.Platform,
        dateConstraints: ContestDateConstraints.Current
    ): List<Contest> {
        return Jsoup.parse(AtCoderApi.getContestsPage())
            .select("time.fixtime-full")
            .mapNotNull { extractContestOrNull(it, dateConstraints) }
    }

    private fun extractContestOrNull(
        timeElement: Element,
        dateConstraints: ContestDateConstraints.Current
    ): Contest? {
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

            if (!dateConstraints.check(startTime, duration)) return null

            val title = td[1].expectFirst("a")

            Contest(
                platform = Contest.Platform.atcoder,
                title = title.text().trim(),
                id = title.attr("href").removePrefix("/contests/"),
                startTime = startTime,
                durationSeconds = duration.inWholeSeconds
            )
        }.getOrNull()
    }

}