package com.demich.cps.platforms.utils

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.alternativeParsing
import kotlinx.datetime.format.char
import kotlinx.datetime.parse
import kotlinx.datetime.toInstant
import org.jsoup.Jsoup
import kotlin.time.Instant

object ProjectEulerUtils {
    private val peUtcOffset get() = UtcOffset(hours = +1)

    class RecentProblem(
        val name: String,
        override val id: String,
        val date: Instant
    ): NewsPostEntry

    fun extractRecentProblems(source: String): List<RecentProblem?> {
        val format = LocalDateTime.Format {
            //5th April 2025, 02:00 pm
            day(padding = Padding.NONE)
            alternativeParsing(
                { chars("st") },
                { chars("nd") },
                { chars("rd") }
            ) { chars("th") }
            char(' ')
            monthName(names = MonthNames.ENGLISH_FULL)
            char(' ')
            year()
            chars(", ")
            amPmHour()
            char(':')
            minute()
            char(' ')
            amPmMarker(am = "am", pm = "pm")
        }

        return Jsoup.parse(source).expectFirst("#problems_table").select("td.id_column")
            .map { idCell ->
                idCell.nextElementSibling()?.let { nameCell ->
                    // title="Published on Saturday, 5th April 2025, 02:00 pm"
                    val title = nameCell.expectFirst("a").attr("title")
                    val dateStr = title.substring(startIndex = title.indexOf(',')+1).trim()
                    RecentProblem(
                        name = nameCell.text(),
                        id = idCell.text(),
                        date = format.parse(dateStr).toInstant(peUtcOffset)
                    )
                }
            }
    }

    class NewsPost(
        val title: String,
        val descriptionHtml: String,
        override val id: String
    ): NewsPostEntry

    fun extractNewsFromRSSPage(rssPage: String): List<NewsPost?> =
        Jsoup.parse(rssPage).select("item")
            .map { item ->
                val idFull = item.expectFirst("guid").text()
                val id = idFull.removePrefix("news_id_")
                if (id != idFull) {
                    NewsPost(
                        title = item.expectFirst("title").text(),
                        descriptionHtml = item.expectFirst("description").html(),
                        id = id
                    )
                } else {
                    null
                }
            }

    fun extractProblemsFromRssPage(rssPage: String): List<Pair<Int, Instant>> {
        val format = DateTimeComponents.Format {
            //04 Apr 2025 23:00:00 +0100
            day(padding = Padding.NONE)
            char(' ')
            monthName(names = MonthNames.ENGLISH_ABBREVIATED)
            char(' ')
            year()
            char(' ')
            time(LocalTime.Formats.ISO)
            char(' ')
            offset(UtcOffset.Formats.FOUR_DIGITS)
        }
        return Jsoup.parse(rssPage).select("item").mapNotNull { item ->
            val idFull = item.expectFirst("guid").text()
            val id = idFull.removePrefix("problem_id_")
            if (id != idFull) {
                val description = item.expectFirst("description").text()
                val date = Instant.parse(
                    input = description.run { substring(startIndex = indexOf(',') + 2) },
                    format = format
                )
                id.toInt() to date
            } else {
                null
            }
        }
    }
}

