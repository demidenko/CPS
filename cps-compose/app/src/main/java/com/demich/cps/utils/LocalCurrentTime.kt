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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

val localCurrentTime: Instant
    @Composable
    @ReadOnlyComposable
    inline get() = LocalCurrentTime.current

val LocalCurrentTime = compositionLocalOf<Instant> {
    throw IllegalAccessException("current time not provided")
}

private fun flowOfTruncatedCurrentTime(seconds: Long): Flow<Instant> {
    require(seconds > 0)
    return flow {
        val period = seconds.seconds
        while (true) {
            val currentTime = getCurrentTime()
            val time = currentTime.truncateBySeconds(seconds)
            emit(time)
            // delay(duration = time + period - currentTime)
            delay(duration = period - (currentTime - time))
        }
    }
}

fun flowOfCurrentTimeEachSecond(): Flow<Instant> =
    flowOfTruncatedCurrentTime(seconds = 1)

@Composable
fun currentTimeAsState(period: Duration): State<Instant> {
    require(period.isPositive())
    val seconds = period.toComponents { seconds: Long, nanoseconds: Int ->
        require(nanoseconds == 0) { "period must be divisible by 1 second"}
        seconds
    }
    return remember(key1 = seconds) {
        flowOfTruncatedCurrentTime(seconds = seconds)
    }.collectAsStateWithLifecycle(initialValue = remember { getCurrentTime().truncateBySeconds(seconds) })
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