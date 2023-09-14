package com.demich.cps.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

val LocalCurrentTime = compositionLocalOf<Instant> {
    throw IllegalAccessException("current time not provided")
}

fun flowOfFlooredCurrentTime(period: Duration): Flow<Instant> =
    flow {
        require(period.isPositive())
        while (currentCoroutineContext().isActive) {
            val currentTime = getCurrentTime()
            emit(currentTime.floorBy(period))
            delay(duration = period - currentTime % period)
        }
    }

@Composable
fun currentTimeAsState(period: Duration): State<Instant> {
    return remember(key1 = period) {
        flowOfFlooredCurrentTime(period = period)
    }.collectAsStateWithLifecycle(initialValue = remember { getCurrentTime().floorBy(period) })
}


@Composable
fun ProvideCurrentTime(currentTime: () -> Instant, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalCurrentTime provides currentTime(), content = content)
}

@Composable
private fun ProvideCurrentTime(currentTimeState: State<Instant>, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalCurrentTime provides currentTimeState.value, content = content)
}

@Composable
fun ProvideTimeEachSecond(content: @Composable () -> Unit) =
    ProvideCurrentTime(currentTimeState = currentTimeAsState(1.seconds), content = content)

@Composable
fun ProvideTimeEachMinute(content: @Composable () -> Unit) =
    ProvideCurrentTime(currentTimeState = currentTimeAsState(1.minutes), content = content)