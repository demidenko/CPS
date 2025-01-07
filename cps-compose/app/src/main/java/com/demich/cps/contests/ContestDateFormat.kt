package com.demich.cps.contests

import androidx.compose.ui.text.intl.Locale
import com.demich.cps.contests.database.Contest
import com.demich.cps.utils.timeDifference
import com.demich.cps.utils.toHHMMSS
import com.demich.cps.utils.toSystemDateTime
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.char
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

fun contestTimeDifference(fromTime: Instant, toTime: Instant): String {
    val t: Duration = toTime - fromTime
    if(t < 48.hours) return t.toHHMMSS()
    return timeDifference(t)
}

private object Formats {
    val HHMM = LocalTime.Format {
        hour()
        char(':')
        minute()
    }

    val ddMM = LocalDate.Format {
        dayOfMonth()
        char('.')
        monthNumber()
    }

    val ddMME = LocalDate.Format {
        date(ddMM)
        char(' ')
        dayOfWeek(
            names = when (Locale.current.language) {
                "ru" -> DayOfWeekNames(listOf("пн", "вт", "ср", "чт", "пт", "сб", "вс"))
                else -> DayOfWeekNames.ENGLISH_ABBREVIATED
            }
        )
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
    val end = if (startLocalDateTime.date == endLocalDateTime.date)
        endLocalDateTime.formatTime() else endLocalDateTime.contestDate()
    return "$start - $end"
}

fun Instant.ratingChangeDate(): String =
    toSystemDateTime().format(LocalDateTime.Format {
        date(Formats.ddMM)
        char('.')
        year()
        char(' ')
        time(Formats.HHMM)
    })