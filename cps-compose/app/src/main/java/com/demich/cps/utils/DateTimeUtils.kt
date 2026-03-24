package com.demich.cps.utils

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.format.DayOfWeekNames
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.Instant


fun Instant.truncateBySeconds(seconds: Long): Instant {
    return Instant.fromEpochSeconds(epochSeconds = epochSeconds - epochSeconds % seconds)
}

fun Clock.flowOfTruncatedCurrentTime(seconds: Long): Flow<Instant> {
    require(seconds > 0)
    return flow {
        val period = seconds.seconds
        while (true) {
            val time = now().truncateBySeconds(seconds)
            emit(time)
            val currentTime = now()
            // delay(duration = time + period - currentTime)
            delay(duration = period - (currentTime - time))
        }
    }
}

private fun Duration.dropSeconds(): Duration = inWholeMinutes.minutes


private fun Duration.formatDHHMMSS(): String = toComponents { days, hours, minutes, seconds, _ ->
    String.format(null, "%d days %02d:%02d:%02d", days, hours, minutes, seconds)
}

private fun Duration.formatHHMMSS(): String = toComponents { hours, minutes, seconds, _ ->
    String.format(null, "%02d:%02d:%02d", hours, minutes, seconds)
}

fun Duration.formatExecTime(): String {
    if (this < 1.seconds) return toString(unit = DurationUnit.MILLISECONDS)
    return toString(unit = DurationUnit.SECONDS, decimals = 1).replace(',', '.')
}

fun Duration.formatDropSeconds(): String =
    1.minutes.let { if (this < it) "<$it" else dropSeconds().toString() }

fun Duration.formatRoundedTime(): String =
    when {
        this < 2.minutes -> "minute"
        this < 2.hours -> "$inWholeMinutes minutes"
        this < 24.hours * 2 -> "$inWholeHours hours"
        this < 31.days * 2 -> "$inWholeDays days"
        this < 365.days * 2 -> "${inWholeDays / 31} months"
        else -> "${inWholeDays / 365} years"
    }

fun Duration.formatTimerShort(): String =
    if (this < 48.hours) formatHHMMSS() else formatRoundedTime()

fun Duration.formatTimerFull(): String =
    if (this < 48.hours) formatHHMMSS() else formatDHHMMSS()

val DayOfWeekNames.Companion.RUSSIAN_ABBREVIATED: DayOfWeekNames
    get() = DayOfWeekNames(listOf("пн", "вт", "ср", "чт", "пт", "сб", "вс"))
