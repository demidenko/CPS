package com.demich.cps.utils

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.format.DayOfWeekNames
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

fun Duration.dropSeconds(): Duration = inWholeMinutes.minutes

fun Instant.truncateBySeconds(): Instant {
    return Instant.fromEpochSeconds(epochSeconds = epochSeconds)
}

fun Instant.truncateBySeconds(seconds: Long): Instant {
    return Instant.fromEpochSeconds(epochSeconds = epochSeconds - epochSeconds % seconds)
}

fun Clock.flowOfTruncatedCurrentTime(seconds: Long): Flow<Instant> {
    require(seconds > 0) { "seconds must be positive" }
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

val DayOfWeekNames.Companion.RUSSIAN_ABBREVIATED: DayOfWeekNames
    get() = DayOfWeekNames(listOf("пн", "вт", "ср", "чт", "пт", "сб", "вс"))
