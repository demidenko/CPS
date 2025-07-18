package com.demich.cps.contests

import com.demich.cps.contests.database.Contest
import com.demich.cps.utils.RUSSIAN_ABBREVIATED
import com.demich.cps.utils.isRuSystemLanguage
import com.demich.cps.utils.toSystemDateTime
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.char
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

private object Formats {
    //TODO: setup date format in setting
    private val dateSeparator = if (isRuSystemLanguage()) '.' else '/'

    val HHMM = LocalTime.Format {
        hour()
        char(':')
        minute()
    }

    val ddMM = LocalDate.Format {
        day()
        char(dateSeparator)
        monthNumber()
    }

    val ddMME = LocalDate.Format {
        date(ddMM)
        char(' ')
        dayOfWeek(names =
            if (isRuSystemLanguage()) DayOfWeekNames.RUSSIAN_ABBREVIATED
            else DayOfWeekNames.ENGLISH_ABBREVIATED
        )
    }

    val ddMMYYYY = LocalDate.Format {
        date(ddMM)
        char(dateSeparator)
        year()
    }
}

private fun LocalDateTime.formatDate() = date.format(Formats.ddMME)
private fun LocalDateTime.formatTime() = time.format(Formats.HHMM)

fun LocalDateTime.contestDate() = "${formatDate()} ${formatTime()}"

fun Contest.dateShortRange(): String {
    val startLocalDateTime = startTime.toSystemDateTime()
    val endLocalDateTime = endTime.toSystemDateTime()
    val start = startLocalDateTime.contestDate()
    val end = if (eventDuration < 24.hours) endLocalDateTime.formatTime() else "..."
    return "$start-$end"
}

fun Contest.dateRange(): String {
    //TODO: show year
    val startLocalDateTime = startTime.toSystemDateTime()
    val endLocalDateTime = endTime.toSystemDateTime()
    val start = startLocalDateTime.contestDate()
    val end = endLocalDateTime.run {
        if (date == startLocalDateTime.date) formatTime() else contestDate()
    }
    return "$start - $end"
}

fun Instant.ratingChangeDate(): String =
    toSystemDateTime().format(LocalDateTime.Format {
        date(Formats.ddMMYYYY)
        char(' ')
        time(Formats.HHMM)
    })