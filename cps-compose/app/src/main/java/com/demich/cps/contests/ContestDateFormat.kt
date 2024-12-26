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
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

fun contestTimeDifference(fromTime: Instant, toTime: Instant): String {
    val t: Duration = toTime - fromTime
    if(t < 48.hours) return t.toHHMMSS()
    return timeDifference(t)
}

private val HHMMFormat = LocalTime.Format {
    hour()
    char(':')
    minute()
}

private val ddMMFormat = LocalDate.Format {
    dayOfMonth()
    char('.')
    monthNumber()
}

private val ruWeekNames by lazy { DayOfWeekNames(listOf("пн", "вт", "ср", "чт", "пт", "сб", "вс")) }

private fun LocalDate.formated(): String = format(LocalDate.Format {
    date(ddMMFormat)
    char(' ')
    dayOfWeek(names = if (Locale.current.language == "ru") ruWeekNames else DayOfWeekNames.ENGLISH_ABBREVIATED)
})

private fun LocalTime.formatted(): String = format(HHMMFormat)

fun LocalDateTime.contestDate() = date.formated() + " " + time.formatted()

fun Contest.dateShortRange(): String {
    val startLocalDateTime = startTime.toSystemDateTime()
    val endLocalDateTime = endTime.toSystemDateTime()
    val start = startLocalDateTime.contestDate()
    val end = if (duration < 1.days) endLocalDateTime.time.formatted() else "..."
    return "$start-$end"
}

fun Contest.dateRange(): String {
    //TODO: show year
    val startLocalDateTime = startTime.toSystemDateTime()
    val endLocalDateTime = endTime.toSystemDateTime()
    val start = startLocalDateTime.contestDate()
    val end = if (startLocalDateTime.date == endLocalDateTime.date)
        endLocalDateTime.time.formatted() else endLocalDateTime.contestDate()
    return "$start - $end"
}

fun Instant.ratingChangeDate(): String =
    toSystemDateTime().format(LocalDateTime.Format {
        date(ddMMFormat)
        char('.')
        year()
        char(' ')
        time(HHMMFormat)
    })