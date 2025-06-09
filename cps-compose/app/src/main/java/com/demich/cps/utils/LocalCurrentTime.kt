package com.demich.cps.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

val localCurrentTime: Instant
    @Composable
    @ReadOnlyComposable
    get() = LocalCurrentTime.current

private val LocalCurrentTime = compositionLocalOf<Instant> {
    throw IllegalAccessException("current time not provided")
}

private fun flowOfTruncatedCurrentTime(period: Duration): Flow<Instant> {
    require(period.isPositive())
    return flow {
        while (true) {
            val currentTime = getCurrentTime()
            emit(currentTime.truncateBy(period))
            delay(duration = period - currentTime % period)
        }
    }
}

fun flowOfCurrentTimeEachSecond(): Flow<Instant> =
    flowOfTruncatedCurrentTime(period = 1.seconds)

@Composable
fun currentTimeAsState(period: Duration): State<Instant> {
    return remember(key1 = period) {
        flowOfTruncatedCurrentTime(period = period)
    }.collectAsStateWithLifecycle(initialValue = remember { getCurrentTime().truncateBy(period) })
}

@Composable
fun ProvideCurrentTime(currentTimeState: State<Instant>, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalCurrentTime provides currentTimeState.value, content = content)
}

@Composable
fun ProvideTimeEachSecond(content: @Composable () -> Unit) =
    ProvideCurrentTime(currentTimeState = currentTimeAsState(1.seconds), content = content)

@Composable
fun ProvideTimeEachMinute(content: @Composable () -> Unit) =
    ProvideCurrentTime(currentTimeState = currentTimeAsState(1.minutes), content = content)


@Composable
@ReadOnlyComposable
fun Instant.toTimeAgoString(): String =
    (localCurrentTime - this).toRoundedTimeString() + " ago"