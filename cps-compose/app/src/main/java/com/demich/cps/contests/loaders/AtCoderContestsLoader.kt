package com.demich.cps.contests.loaders

import com.demich.cps.contests.Contest
import com.demich.cps.utils.AtCoderApi
import kotlinx.datetime.Instant
import org.jsoup.Jsoup
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class AtCoderContestsLoader: ContestsLoader(type = ContestsLoaders.atcoder) {
    override suspend fun loadContests(platform: Contest.Platform): List<Contest> {
        return Jsoup.parse(AtCoderApi.getContestsPage()).select("time.fixtime-full").map { timeElement ->
            val row = timeElement.parents().find { it.normalName() == "tr" }!!
            val td = row.select("td")

            val title = td[1].selectFirst("a")!!

            //YYYY-MM-DD hh:mm:ss+0900
            val timeString = timeElement.text()
            val startTime = Instant.parse(timeString.replace("+0900", "+09:00").replace(' ', 'T'))

            val durationSeconds = td[2].text().split(':').let {
                val h = it[0].toInt()
                val m = it[1].toInt()
                (h.hours + m.minutes).inWholeSeconds
            }

            Contest(
                platform = Contest.Platform.atcoder,
                title = title.text().trim(),
                id = title.attr("href").removePrefix("/contests/"),
                startTime = startTime,
                durationSeconds = durationSeconds
            )
        }
    }

}