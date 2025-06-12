package com.demich.cps.contests.monitors

import androidx.compose.runtime.Immutable
import com.demich.cps.platforms.api.codeforces.models.CodeforcesContest
import com.demich.cps.platforms.api.codeforces.models.CodeforcesContestPhase
import com.demich.cps.platforms.api.codeforces.models.CodeforcesParticipationType
import kotlinx.datetime.Instant

@Immutable
data class CodeforcesMonitorData(
    val contestInfo: CodeforcesContest,
    val contestPhase: ContestPhase,
    val contestantRank: ContestRank,
    val problems: List<ProblemInfo>
) {
    val contestId: Int get() = contestInfo.id

    sealed interface ContestPhase {
        val phase: CodeforcesContestPhase

        data class Coding(val endTime: Instant): ContestPhase {
            override val phase get() = CodeforcesContestPhase.CODING
        }

        data class SystemTesting(val percentage: Int?): ContestPhase {
            override val phase get() = CodeforcesContestPhase.SYSTEM_TEST
        }

        data class Other(override val phase: CodeforcesContestPhase): ContestPhase
    }

    data class ContestRank(
        val rank: Int?,
        val participationType: CodeforcesParticipationType
    )

    data class ProblemInfo(
        val name: String,
        val result: ProblemResult
    )

    sealed interface ProblemResult {
        data object Empty: ProblemResult
        data object Pending: ProblemResult
        data object FailedSystemTest: ProblemResult
        data class Points(val points: Double, val isFinal: Boolean): ProblemResult {
            fun pointsToNiceString() = points.toString().removeSuffix(".0")
        }

        companion object {
            const val failedSystemTestSymbol = "⨯" //alternatives: × ✖
        }
    }
}