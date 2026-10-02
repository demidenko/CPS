package com.demich.cps.contests

import com.demich.cps.contests.database.Contest
import com.demich.cps.ui.CPSDateFormats
import com.demich.cps.ui.formatFullDateTime
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.hours

private fun LocalDateTime.formatTime() = time.format(CPSDateFormats.HHMM)

context(timeZone: TimeZone)
fun Contest.formatDateRangeCompact(): String {
    require(startTime <= endTime)

    val startLocalDateTime = startTime.toLocalDateTime(timeZone)
    val start = startLocalDateTime.formatFullDateTime(showDayOfWeek = true, showYear = false)
    if (startTime == endTime) return start

    val endLocalDateTime = endTime.toLocalDateTime(timeZone)
    val end = if (eventDuration < 24.hours) endLocalDateTime.formatTime() else "..."

    return "$start-$end"
}

context(timeZone: TimeZone)
fun Contest.formatDateRange(
    showDayOfWeek: Boolean
): String {
    require(startTime <= endTime)

    //TODO: show year
    val showYear = false

    val startLocalDateTime = startTime.toLocalDateTime(timeZone)
    val start = startLocalDateTime.formatFullDateTime(showDayOfWeek = showDayOfWeek, showYear = showYear)
    if (startTime == endTime) return start

    endTime.toLocalDateTime(timeZone).run {
        return if (date == startLocalDateTime.date) {
            "$start-${formatTime()}"
        } else {
            "$start - ${formatFullDateTime(showDayOfWeek = showDayOfWeek, showYear = showYear)}"
        }
    }
}
