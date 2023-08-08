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
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

val LocalCurrentTime = compositionLocalOf { getCurrentTime() }

private fun currentTimeFlow(period: Duration): Flow<Instant> =
    flow {
        val periodMillis = period.inWholeMilliseconds
        require(periodMillis > 0)
        while (currentCoroutineContext().isActive) {
            val currentMillis = getCurrentTime().toEpochMilliseconds()
            val rem = currentMillis % periodMillis
            emit(Instant.fromEpochMilliseconds(currentMillis - rem))
            delay(timeMillis = if (rem == 0L) periodMillis else periodMillis - rem)
        }
    }

@Composable
fun collectCurrentTimeAsState(period: Duration): State<Instant> {
    return remember(key1 = period) {
        currentTimeFlow(period = period)
    }.collectAsStateWithLifecycle(initialValue = remember(::getCurrentTime))
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