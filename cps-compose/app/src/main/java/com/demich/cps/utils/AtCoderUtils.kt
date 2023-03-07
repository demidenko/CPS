package com.demich.cps.utils

import com.demich.cps.accounts.managers.AtCoderUserInfo
import com.demich.cps.accounts.managers.STATUS
import com.demich.cps.contests.Contest
import io.ktor.client.request.*
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

object AtCoderApi: ResourceApi {
    private val json get() = defaultJson

    suspend fun getUserPage(handle: String): String  {
        return client.getText(urlString = urls.user(handle)) {
            parameter("graph", "rating")
        }
    }

    suspend fun getMainPage(): String  {
        return client.getText(urlString = urls.main)
    }

    suspend fun getContestsPage(): String {
        return client.getText(urlString = urls.main + "/contests")
    }

    suspend fun getRatingChanges(handle: String): List<AtCoderRatingChange> {
        val src = getUserPage(handle)
        val i = src.lastIndexOf("<script>var rating_history=[{")
        if (i == -1) return emptyList()
        val j = src.indexOf("];</script>", i)
        val str = src.substring(src.indexOf('[', i), j+1)
        return json.decodeFromString(str)
    }

    object urls {
        const val main = "https://atcoder.jp"
        fun user(handle: String) = "$main/users/$handle"
        fun userContestResult(handle: String, contestId: String) = "$main/users/$handle/history/share/$contestId"
        fun contest(id: String) = "$main/contests/$id"
        fun post(id: Int) = "$main/posts/$id"
    }
}


@Serializable
data class AtCoderRatingChange(
    val NewRating: Int,
    val OldRating: Int,
    val Place: Int,
    @Serializable(with = InstantAsSecondsSerializer::class)
    val EndTime: Instant,
    val ContestName: String,
    val StandingsUrl: String
) {
    fun getContestId(): String {
        val s = StandingsUrl.removePrefix("/contests/")
        return s.substring(0, s.indexOf('/'))
    }
}


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