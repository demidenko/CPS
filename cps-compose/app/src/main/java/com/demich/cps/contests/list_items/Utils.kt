package com.demich.cps.contests.list_items

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
    if(t < 24.hours * 2) return t.toHHMMSS()
    return timeDifference(fromTime, toTime)
}

internal fun Instant.contestDate() = format("dd.MM E HH:mm")

internal fun Contest.dateShortRange(): String {
    val start = startTime.contestDate()
    val end = if (duration < 1.days) endTime.format("HH:mm") else "..."
    return "$start-$end"
}

internal fun Contest.dateRange(): String {
    //TODO: smart shrink
    //TODO: show year
    return startTime.contestDate() + " - " + endTime.contestDate()
}