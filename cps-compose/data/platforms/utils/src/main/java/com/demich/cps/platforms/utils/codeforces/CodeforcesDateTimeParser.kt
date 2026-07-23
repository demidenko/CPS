package com.demich.cps.platforms.utils.codeforces

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.alternativeParsing
import kotlinx.datetime.format.char
import kotlinx.datetime.toInstant
import org.jsoup.nodes.Element
import kotlin.time.Instant

private val moscowTimeZone = TimeZone.of("Europe/Moscow")

private val dateTimeFormat = LocalDateTime.Format {
    alternativeParsing({
        //RU format: "dd.MM.yyyy HH:mm"
        day()
        char('.')
        monthNumber()
        char('.')
        year()
    }) {
        //EN format: "MMM/dd/yyyy HH:mm"
        monthName(MonthNames.ENGLISH_ABBREVIATED)
        char('/')
        day()
        char('/')
        year()
    }
    char(' ')
    hour()
    char(':')
    minute()
}

context(parser: CodeforcesHtmlParser)
internal fun Element.extractTime(): Instant =
    LocalDateTime.parse(input = attr("title"), format = dateTimeFormat).toInstant(moscowTimeZone)