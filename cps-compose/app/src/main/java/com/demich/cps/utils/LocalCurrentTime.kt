package com.demich.cps.utils

import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private operator fun Instant.rem(period: Duration): Duration {
    val periodMillis = period.inWholeMilliseconds
    val thisMillis = toEpochMilliseconds()
    return (thisMillis % periodMillis).milliseconds
}

private fun Instant.floorBy(period: Duration): Instant = this - this % period



val LocalCurrentTime = compositionLocalOf<Instant> { throw IllegalAccessException() }

private fun flowOfFlooredCurrentTime(period: Duration): Flow<Instant> =
    flow {
        while (currentCoroutineContext().isActive) {
            val currentTime = getCurrentTime()
            emit(currentTime.floorBy(period))
            delay(duration = period - currentTime % period)
        }
    }

@Composable
fun collectCurrentTimeAsState(period: Duration): State<Instant> {
    require(period.isPositive())
    return remember(key1 = period) {
        flowOfFlooredCurrentTime(period = period)
    }.collectAsStateWithLifecycle(initialValue = remember { getCurrentTime().floorBy(period) })
}

@Composable
private fun ProvideCurrentTime(period: Duration, content: @Composable () -> Unit) {
    val currentTime by collectCurrentTimeAsState(period)
    CompositionLocalProvider(LocalCurrentTime provides currentTime, content = content)
}

@Composable
fun ProvideTimeEachSecond(content: @Composable () -> Unit) =
    ProvideCurrentTime(period = 1.seconds, content = content)

@Composable
fun ProvideTimeEachMinute(content: @Composable () -> Unit) =
    ProvideCurrentTime(period = 1.minutes, content = content)