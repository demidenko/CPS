package com.demich.cps.contests

import com.demich.cps.contests.database.Contest
import com.demich.cps.utils.format
import com.demich.cps.utils.timeDifference
import com.demich.cps.utils.toHHMMSS
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

internal fun contestTimeDifference(fromTime: Instant, toTime: Instant): String {
    val t: Duration = toTime - fromTime
    if(t < 48.hours) return t.toHHMMSS()
    return timeDifference(t)
}

private fun Instant.formatDate() = format("dd.MM E")
private fun Instant.formatTime() = format("HH:mm")

internal fun Instant.contestDate() = formatDate() + " " + formatTime()

internal fun Contest.dateShortRange(): String {
    val start = startTime.contestDate()
    val end = if (duration < 1.days) endTime.formatTime() else "..."
    return "$start-$end"
}

internal fun Contest.dateRange(): String {
    //TODO: show year
    val start = startTime.contestDate()
    val end = if (startTime.formatDate() == endTime.formatDate())
        endTime.formatTime() else endTime.contestDate()
    return "$start - $end"
}