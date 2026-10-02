package com.demich.cps.contests

import com.demich.cps.contests.database.Contest
import com.demich.cps.ui.CPSDateFormats
import com.demich.cps.utils.getSystemTimeZone
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.hours

private fun LocalDateTime.formatDate() = date.format(CPSDateFormats.ddMME)
private fun LocalDateTime.formatTime() = time.format(CPSDateFormats.HHMM)

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
