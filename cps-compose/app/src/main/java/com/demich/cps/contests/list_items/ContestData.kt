package com.demich.cps.contests.list_items

import androidx.compose.runtime.Immutable
import com.demich.cps.contests.database.Contest
import kotlinx.datetime.Instant

@Immutable
internal data class ContestData(
    val contest: Contest,
    val phase: Contest.Phase,
    val startsIn: String,
    val endsIn: String
) {
    constructor(contest: Contest, currentTime: Instant): this(
        contest = contest,
        phase = contest.getPhase(currentTime),
        startsIn = contestTimeDifference(currentTime, contest.startTime),
        endsIn = contestTimeDifference(currentTime, contest.endTime)
    )
}