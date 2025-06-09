package com.demich.cps.utils

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

fun getCurrentTime() = Clock.System.now()

fun Instant.toSystemDateTime(): LocalDateTime =
    toLocalDateTime(timeZone = TimeZone.currentSystemDefault())

operator fun Instant.rem(period: Duration): Duration {
    val periodMillis = period.inWholeMilliseconds
    val thisMillis = toEpochMilliseconds()
    return (thisMillis % periodMillis).milliseconds
}

fun Instant.truncateBy(period: Duration): Instant = this - this % period


fun Duration.toHHMMSS(): String = toComponents { hours, minutes, seconds, _ ->
    String.format(null, "%02d:%02d:%02d", hours, minutes, seconds)
}

fun Duration.toMMSS(): String = toComponents { minutes, seconds, _ ->
    String.format(null, "%02d:%02d", minutes, seconds)
}

fun timeDifference(t: Duration): String =
    when {
        t < 2.minutes -> "minute"
        t < 2.hours -> "${t.inWholeMinutes} minutes"
        t < 24.hours * 2 -> "${t.inWholeHours} hours"
        t < 31.days * 2 -> "${t.inWholeDays} days"
        t < 365.days * 2 -> "${t.inWholeDays / 31} months"
        else -> "${t.inWholeDays / 365} years"
    }

fun timeDifference(fromTime: Instant, toTime: Instant) = timeDifference(toTime - fromTime)

