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
import org.jsoup.nodes.Element
import kotlin.time.Instant

class ProjectEulerParser {
    fun parseRecentProblems(source: String): List<ProjectEulerRecentProblem?> {
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

        val peUtcOffset = UtcOffset(hours = +1)

        return source.parseDocument().expectFirst("#problems_table").select("td.id_column")
            .map { idCell ->
                idCell.nextElementSibling()?.let { nameCell ->
                    // title="Published on Saturday, 5th April 2025, 02:00 pm"
                    val title = nameCell.expectFirst("a").attr("title")
                    val dateStr = title.substring(startIndex = title.indexOf(',')+1).trim()
                    ProjectEulerRecentProblem(
                        name = nameCell.text(),
                        id = idCell.text(),
                        date = format.parse(dateStr).toInstant(peUtcOffset)
                    )
                }
            }
    }
}

class ProjectEulerRssParser(
    rssPage: String
) {
    private val items: Sequence<RssItem> =
        rssPage.parseDocument().selectSequence("item").map { RssItem(it) }

    private class RssItem(private val item: Element) {
        fun description() = item.expectFirst("description")

        fun title() = item.expectFirst("title")

        inline fun <T> guidOrNull(prefix: String, block: (String) -> T): T? {
            val idFull = item.expectFirst("guid").text()
            val id = idFull.removePrefix(prefix)
            if (id == idFull) return null
            return block(id)
        }
    }


    fun parseNews(): Sequence<ProjectEulerNewsPost> =
        items.mapNotNull { item ->
            item.guidOrNull(prefix = "news_id_") { id ->
                ProjectEulerNewsPost(
                    title = item.title().text(),
                    descriptionHtml = item.description().html(),
                    id = id
                )
            }
        }

    fun parseProblems(): Sequence<Pair<Int, Instant>> {
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

        return items.mapNotNull { item ->
            item.guidOrNull(prefix = "problem_id_") { id ->
                val description = item.description().text()
                val date = Instant.parse(
                    input = description.run { substring(startIndex = indexOf(',') + 2) },
                    format = format
                )
                id.toInt() to date
            }
        }
    }
}

class ProjectEulerRecentProblem(
    val name: String,
    override val id: String,
    val date: Instant
): NewsPostEntry

class ProjectEulerNewsPost(
    val title: String,
    val descriptionHtml: String,
    override val id: String
): NewsPostEntry