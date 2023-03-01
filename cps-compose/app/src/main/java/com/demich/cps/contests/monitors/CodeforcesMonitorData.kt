package com.demich.cps.contests.monitors

import androidx.compose.runtime.Immutable
import com.demich.cps.utils.codeforces.CodeforcesContest
import com.demich.cps.utils.codeforces.CodeforcesContestPhase
import com.demich.cps.utils.codeforces.CodeforcesParticipationType
import kotlinx.datetime.Instant

@Immutable
data class CodeforcesMonitorData(
    val contestInfo: CodeforcesContest,
    val contestPhase: ContestPhase,
    val contestantRank: ContestRank,
    val lastRequest: Boolean?
) {
    sealed class ContestPhase(val phase: CodeforcesContestPhase) {
        data class Coding(val endTime: Instant): ContestPhase(CodeforcesContestPhase.CODING)
        data class SystemTesting(val percentage: Int?): ContestPhase(CodeforcesContestPhase.SYSTEM_TEST)
        class Other(phase: CodeforcesContestPhase): ContestPhase(phase)
    }

    data class ContestRank(
        val rank: Int,
        val participationType: CodeforcesParticipationType
    )
}