package com.demich.cps.contests.list_items

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import com.demich.cps.contests.contestTimeDifference
import com.demich.cps.contests.database.Contest
import com.demich.cps.utils.DangerType
import com.demich.cps.utils.localCurrentTime

@Immutable
internal data class ContestData(
    val contest: Contest,
    val phase: Contest.Phase,
    val counter: String,
    val collisionType: DangerType
)

@Composable
@ReadOnlyComposable
internal fun ContestDisplay.dataByCurrentTime(): ContestData {
    //TODO: call recomposes two times
    val currentTime = localCurrentTime
    val phase = contest.getPhase(currentTime)
    val counter = when (phase) {
        Contest.Phase.BEFORE -> contestTimeDifference(currentTime, contest.startTime) //startsIn
        Contest.Phase.RUNNING -> contestTimeDifference(currentTime, contest.endTime) //endsIn
        Contest.Phase.FINISHED -> ""
    }
    return ContestData(contest, phase, counter, collisionType)
}


@Immutable
internal data class ContestDisplay(
    val contest: Contest,
    val collisionType: DangerType
)

internal fun Contest.platformName() = host ?: platform.name