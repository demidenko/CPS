package com.demich.cps.contests.list_items

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.demich.cps.contests.database.Contest
import com.demich.cps.utils.localCurrentTime
import kotlin.time.Duration
import kotlin.time.Instant

internal inline fun Contest.counterAt(
    time: Instant,
    upcoming: (Duration) -> String,
    running: (Duration) -> String,
    finished: () -> String
): String =
    when (phaseAt(time)) {
        UPCOMING -> upcoming(startTime - time)
        RUNNING -> running(endTime - time)
        FINISHED -> finished()
    }

@Composable
@ReadOnlyComposable
internal inline fun Contest.localCurrentCounter(
    upcoming: (Duration) -> String,
    running: (Duration) -> String,
    finished: () -> String = { "" }
): String =
    counterAt(
        time = localCurrentTime,
        upcoming = upcoming,
        running = running,
        finished = finished
    )