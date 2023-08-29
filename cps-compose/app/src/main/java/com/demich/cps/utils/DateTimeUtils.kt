package com.demich.cps.utils

import android.text.format.DateFormat
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

fun getCurrentTime() = Clock.System.now()

operator fun Instant.rem(period: Duration): Duration {
    val periodMillis = period.inWholeMilliseconds
    val thisMillis = toEpochMilliseconds()
    return (thisMillis % periodMillis).milliseconds
}

fun Instant.floorBy(period: Duration): Instant = this - this % period


fun Instant.format(dateFormat: String): String =
    DateFormat.format(dateFormat, toEpochMilliseconds()).toString()

fun Duration.toHHMMSS(): String = toComponents { hours, minutes, seconds, _ ->
    String.format("%02d:%02d:%02d", hours, minutes, seconds)
}

fun Duration.toMMSS(): String = toComponents { minutes, seconds, _ ->
    String.format("%02d:%02d", minutes, seconds)
}

fun timeDifference(fromTime: Instant, toTime: Instant): String {
    val t: Duration = toTime - fromTime
    return when {
        t < 2.minutes -> "minute"
        t < 2.hours -> "${t.inWholeMinutes} minutes"
        t < 24.hours * 2 -> "${t.inWholeHours} hours"
        t < 7.days * 2 -> "${t.inWholeDays} days"
        t < 31.days * 2 -> "${t.inWholeDays / 7} weeks"
        t < 365.days * 2 -> "${t.inWholeDays / 31} months"
        else -> "${t.inWholeDays / 365} years"
    }
}

fun timeAgo(fromTime: Instant, toTime: Instant) = timeDifference(fromTime, toTime) + " ago"
