package com.demich.cps.utils

import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.monthsUntil
import kotlinx.datetime.yearsUntil
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

private fun Duration.formatDHHMMSS(): String = toComponents { days, hours, minutes, seconds, _ ->
    String.format(null, "%d days %02d:%02d:%02d", days, hours, minutes, seconds)
}

private fun Duration.formatHHMMSS(): String = toComponents { hours, minutes, seconds, _ ->
    String.format(null, "%02d:%02d:%02d", hours, minutes, seconds)
}

fun Duration.formatExecTime(): String {
    if (this < 1.seconds) return toString(unit = MILLISECONDS)
    return toString(unit = SECONDS, decimals = 1).replace(',', '.')
}

fun Duration.formatDropSeconds(): String =
    1.minutes.let { if (this < it) "<$it" else dropSeconds().toString() }

private fun Duration.formatRoundedTime(): String =
    when {
        this < 2.minutes -> "minute"
        this < 2.hours -> "$inWholeMinutes minutes"
        this < 24.hours * 2 -> "$inWholeHours hours"
        this < 31.days * 2 -> "$inWholeDays days"
        this < 365.days * 2 -> "${inWholeDays / 31} months"
        else -> "${inWholeDays / 365} years"
    }

context(timeZone: TimeZone)
fun Instant.formatRoundedTime(until: Instant): String {
    daysUntil(until, timeZone = timeZone).let { days ->
        if (days > 0) monthsUntil(until, timeZone = timeZone).let { months ->
            if (months > 0) yearsUntil(until, timeZone = timeZone).let { years ->
                if (years > 1) return "$years years"
            }
            if (months > 1) return "$months months"
        }
        if (days > 1) return "$days days"
    }

    return (until - this).formatRoundedTime()
}

fun Duration.formatTimerShort(): String =
    if (this < 48.hours) formatHHMMSS() else formatRoundedTime()

fun Duration.formatTimerFull(): String =
    if (this < 48.hours) formatHHMMSS() else formatDHHMMSS()