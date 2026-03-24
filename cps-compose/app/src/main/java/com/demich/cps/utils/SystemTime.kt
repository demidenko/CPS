package com.demich.cps.utils

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

fun getSystemTime(): Instant = Clock.System.now()

fun Clock.flowOfTruncatedCurrentTime(seconds: Long): Flow<Instant> {
    require(seconds > 0)
    return flow {
        val period = seconds.seconds
        while (true) {
            val currentTime = now()
            val time = currentTime.truncateBySeconds(seconds)
            emit(time)
            // delay(duration = time + period - currentTime)
            delay(duration = period - (currentTime - time))
        }
    }
}

fun flowOfSystemTimeEachSecond(): Flow<Instant> =
    Clock.System.flowOfTruncatedCurrentTime(seconds = 1)