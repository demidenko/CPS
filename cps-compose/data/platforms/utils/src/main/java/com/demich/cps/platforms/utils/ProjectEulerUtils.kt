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
import org.jsoup.nodes.Element
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

    private class RssItem(private val item: Element) {
        val description get() = item.expectFirst("description")

        val title get() = item.expectFirst("title")

        inline fun <T> guidOrNull(prefix: String, block: (String) -> T): T? {
            val idFull = item.expectFirst("guid").text()
            val id = idFull.removePrefix(prefix)
            if (id == idFull) return null
            return block(id)
        }
    }

    private fun extractRssItems(rssPage: String) =
        Jsoup.parse(rssPage).select("item").map { RssItem(it) }

    class NewsPost(
        val title: String,
        val descriptionHtml: String,
        override val id: String
    ): NewsPostEntry

    fun extractNewsFromRSSPage(rssPage: String): List<NewsPost> =
        extractRssItems(rssPage).mapNotNull { item ->
            item.guidOrNull(prefix = "news_id_") { id ->
                NewsPost(
                    title = item.title.text(),
                    descriptionHtml = item.description.html(),
                    id = id
                )
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
        return extractRssItems(rssPage).mapNotNull { item ->
            item.guidOrNull(prefix = "problem_id_") { id ->
                val description = item.description.text()
                val date = Instant.parse(
                    input = description.run { substring(startIndex = indexOf(',') + 2) },
                    format = format
                )
                id.toInt() to date
            }
        }
    }
}

