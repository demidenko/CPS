package com.demich.cps.contests.list_items

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import com.demich.cps.contests.contestTimeDifference
import com.demich.cps.contests.database.Contest
import com.demich.cps.utils.localCurrentTime

@Immutable
internal data class ContestData(
    val phase: Contest.Phase,
    val counter: String
)

@Composable
@ReadOnlyComposable
internal fun dataByCurrentTime(contest: Contest): ContestData {
    //TODO: call recomposes two times
    val currentTime = localCurrentTime
    val phase = contest.getPhase(currentTime)
    val counter = when (phase) {
        Contest.Phase.BEFORE -> contestTimeDifference(currentTime, contest.startTime) //startsIn
        Contest.Phase.RUNNING -> contestTimeDifference(currentTime, contest.endTime) //endsIn
        Contest.Phase.FINISHED -> ""
    }
    return ContestData(
        phase = phase,
        counter = counter
    )
}

internal fun Contest.platformName() = host ?: platform.name