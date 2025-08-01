package com.demich.cps.utils

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.Instant

fun getCurrentTime(): Instant = kotlin.time.Clock.System.now()

fun Instant.toSystemDateTime(): LocalDateTime =
    toLocalDateTime(timeZone = TimeZone.currentSystemDefault())

fun Instant.truncateBySeconds(seconds: Long): Instant {
    return Instant.fromEpochSeconds(epochSeconds = epochSeconds - epochSeconds % seconds)
}

private fun Duration.dropSeconds(): Duration = inWholeMinutes.minutes


private fun Duration.toDHHMMSS(): String = toComponents { days, hours, minutes, seconds, _ ->
    String.format(null, "%d days %02d:%02d:%02d", days, hours, minutes, seconds)
}

private fun Duration.toHHMMSS(): String = toComponents { hours, minutes, seconds, _ ->
    String.format(null, "%02d:%02d:%02d", hours, minutes, seconds)
}

fun Duration.toExecTimeString(): String {
    if (this < 1.seconds) return toString(unit = DurationUnit.MILLISECONDS)
    return toString(unit = DurationUnit.SECONDS, decimals = 1).replace(',', '.')
}

fun Duration.toDropSecondsString(): String =
    1.minutes.let { if (this < it) "<$it" else dropSeconds().toString() }

fun Duration.toRoundedTimeString(): String =
    when {
        this < 2.minutes -> "minute"
        this < 2.hours -> "$inWholeMinutes minutes"
        this < 24.hours * 2 -> "$inWholeHours hours"
        this < 31.days * 2 -> "$inWholeDays days"
        this < 365.days * 2 -> "${inWholeDays / 31} months"
        else -> "${inWholeDays / 365} years"
    }

fun Duration.timerShort(): String =
    if (this < 48.hours) toHHMMSS() else toRoundedTimeString()

fun Duration.timerFull(): String =
    if (this < 48.hours) toHHMMSS() else toDHHMMSS()

val DayOfWeekNames.Companion.RUSSIAN_ABBREVIATED: DayOfWeekNames
    get() = DayOfWeekNames(listOf("пн", "вт", "ср", "чт", "пт", "сб", "вс"))
