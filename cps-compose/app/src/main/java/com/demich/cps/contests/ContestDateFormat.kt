package com.demich.cps.contests

import com.demich.cps.contests.database.Contest
import com.demich.cps.utils.RUSSIAN_ABBREVIATED
import com.demich.cps.utils.getSystemTimeZone
import com.demich.cps.utils.isRuSystemLanguage
import com.demich.cps.utils.toSystemDateTime
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
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

fun LocalDateTime.formatContestDate() = "${formatDate()} ${formatTime()}"

// TODO: rework to context(timezone)
fun Contest.dateBriefRange(): String = dateBriefRange(timeZone = getSystemTimeZone())

private fun Contest.dateBriefRange(timeZone: TimeZone): String {
    require(startTime <= endTime)

    val startLocalDateTime = startTime.toLocalDateTime(timeZone)
    val start = startLocalDateTime.formatContestDate()
    if (startTime == endTime) return start

    val endLocalDateTime = endTime.toLocalDateTime(timeZone)
    val end = if (eventDuration < 24.hours) endLocalDateTime.formatTime() else "..."

    return "$start-$end"
}

// TODO: rework to context(timezone)
fun Contest.dateRange(): String = dateRange(timeZone = getSystemTimeZone())

private fun Contest.dateRange(timeZone: TimeZone): String {
    require(startTime <= endTime)

    //TODO: show year
    val startLocalDateTime = startTime.toLocalDateTime(timeZone)
    val start = startLocalDateTime.formatContestDate()
    if (startTime == endTime) return start

    val endLocalDateTime = endTime.toLocalDateTime(timeZone)
    val end = endLocalDateTime.run {
        if (date == startLocalDateTime.date) formatTime() else formatContestDate()
    }

    return "$start - $end"
}

fun Instant.formatRatingChangeDate(): String =
    toSystemDateTime().format(LocalDateTime.Format {
        date(Formats.ddMMYYYY)
        char(' ')
        time(Formats.HHMM)
    })