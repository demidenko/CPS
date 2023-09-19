package com.demich.cps.contests.list_items

import androidx.compose.runtime.Immutable
import com.demich.cps.contests.contestTimeDifference
import com.demich.cps.contests.database.Contest
import kotlinx.datetime.Instant

@Immutable
internal data class ContestData(
    val contest: Contest,
    val phase: Contest.Phase,
    val counter: String
)

internal fun contestData(contest: Contest, currentTime: Instant): ContestData {
    val phase = contest.getPhase(currentTime)
    val counter = when (phase) {
        Contest.Phase.BEFORE -> contestTimeDifference(currentTime, contest.startTime) //startsIn
        Contest.Phase.RUNNING -> contestTimeDifference(currentTime, contest.endTime) //endsIn
        Contest.Phase.FINISHED -> ""
    }
    return ContestData(contest, phase, counter)
}