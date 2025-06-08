package com.demich.cps.contests.list_items

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.demich.cps.contests.contestTimeDifference
import com.demich.cps.contests.database.Contest
import com.demich.cps.utils.localCurrentTime

internal fun Contest.platformName() = host ?: platform.name

@Composable
@ReadOnlyComposable
internal inline fun Contest.counter(
    phase: Contest.Phase,
    before: (String) -> String,
    running: (String) -> String,
    finished: () -> String = { "" }
): String =
    when (phase) {
        Contest.Phase.BEFORE -> before(contestTimeDifference(localCurrentTime, startTime))
        Contest.Phase.RUNNING -> running(contestTimeDifference(localCurrentTime, endTime))
        Contest.Phase.FINISHED -> finished()
    }
