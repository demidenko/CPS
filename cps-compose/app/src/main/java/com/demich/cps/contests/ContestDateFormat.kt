package com.demich.cps.contests

import com.demich.cps.contests.database.Contest
import com.demich.cps.ui.CPSDateFormats
import com.demich.cps.utils.getSystemTimeZone
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.hours

private fun LocalDateTime.formatTime() = time.format(CPSDateFormats.HHMM)

private fun LocalDateTime.formatDate(showDayOfWeek: Boolean) =
    date.format(if (showDayOfWeek) CPSDateFormats.ddMME else CPSDateFormats.ddMM)

fun LocalDateTime.formatContestDate(showDayOfWeek: Boolean) = "${formatDate(showDayOfWeek = showDayOfWeek)} ${formatTime()}"

// TODO: rework to context(timezone)
fun Contest.formatDateRangeCompact(): String = formatDateRangeCompact(timeZone = getSystemTimeZone())

private fun Contest.formatDateRangeCompact(timeZone: TimeZone): String {
    require(startTime <= endTime)

    val startLocalDateTime = startTime.toLocalDateTime(timeZone)
    val start = startLocalDateTime.formatContestDate(showDayOfWeek = true)
    if (startTime == endTime) return start

    val endLocalDateTime = endTime.toLocalDateTime(timeZone)
    val end = if (eventDuration < 24.hours) endLocalDateTime.formatTime() else "..."

    return "$start-$end"
}

// TODO: rework to context(timezone)
fun Contest.formatDateRange(showDayOfWeek: Boolean): String =
    formatDateRange(
        timeZone = getSystemTimeZone(),
        showDayOfWeek = showDayOfWeek
    )

private fun Contest.formatDateRange(
    timeZone: TimeZone,
    showDayOfWeek: Boolean
): String {
    require(startTime <= endTime)

    //TODO: show year
    val startLocalDateTime = startTime.toLocalDateTime(timeZone)
    val start = startLocalDateTime.formatContestDate(showDayOfWeek = showDayOfWeek)
    if (startTime == endTime) return start

    endTime.toLocalDateTime(timeZone).run {
        return if (date == startLocalDateTime.date) {
            "$start-${formatTime()}"
        } else {
            "$start - ${formatContestDate(showDayOfWeek = showDayOfWeek)}"
        }
    }
}
