package com.demich.cps.contests.list_items

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.demich.cps.contests.database.Contest
import com.demich.cps.utils.localCurrentTime
import kotlin.time.Duration

@Composable
@ReadOnlyComposable
internal inline fun Contest.counter(
    phase: Contest.Phase,
    before: (Duration) -> String,
    running: (Duration) -> String,
    finished: () -> String = { "" }
): String =
    when (phase) {
        Contest.Phase.BEFORE -> before(startTime - localCurrentTime)
        Contest.Phase.RUNNING -> running(endTime - localCurrentTime)
        Contest.Phase.FINISHED -> finished()
    }
