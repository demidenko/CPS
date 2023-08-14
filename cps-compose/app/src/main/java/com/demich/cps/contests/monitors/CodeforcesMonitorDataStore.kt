package com.demich.cps.contests.monitors

import android.content.Context
import com.demich.cps.platforms.api.CodeforcesContest
import com.demich.cps.platforms.api.CodeforcesContestPhase
import com.demich.cps.platforms.api.CodeforcesParticipationType
import com.demich.cps.platforms.api.CodeforcesProblemStatus
import com.demich.cps.platforms.api.CodeforcesProblemVerdict
import com.demich.cps.platforms.api.CodeforcesSubmission
import com.demich.cps.platforms.api.CodeforcesTestset
import com.demich.cps.utils.jsonCPS
import com.demich.datastore_itemized.ItemizedDataStore
import com.demich.datastore_itemized.dataStoreWrapper
import com.demich.datastore_itemized.flowBy
import kotlinx.coroutines.flow.Flow

class CodeforcesMonitorDataStore(context: Context): ItemizedDataStore(context.cf_monitor_dataStore) {
    companion object {
        private val Context.cf_monitor_dataStore by dataStoreWrapper(name = "cf_monitor")
    }

    val contestId = itemIntNullable(name = "contest_id")
    val handle = itemString(name = "handle", defaultValue = "")

    val lastRequest = jsonCPS.item<Boolean?>(name = "last_request", defaultValue = null)

    internal val contestInfo = jsonCPS.item<CodeforcesContest?>(
        name = "contest_info",
        defaultValue = null
    )

    internal val participationType = itemEnum(name = "participation_type", defaultValue = CodeforcesParticipationType.NOT_PARTICIPATED)
    internal val contestantRank = itemInt(name = "contestant_rank", defaultValue = -1)

    internal val problemResults = jsonCPS.itemList<CodeforcesMonitorProblemResult>(name = "problem_results")

    internal val submissionsInfo = jsonCPS.itemMap<String, List<CodeforcesMonitorSubmissionInfo>>(
        name = "problems_submissions_info"
    )

    internal val sysTestPercentage = itemIntNullable("sys_test_percentage")

    internal val notifiedSubmissionsIds = jsonCPS.itemSet<Long>(name = "submissions_notified")

    suspend fun reset() = resetAll()
}

@kotlinx.serialization.Serializable
internal data class CodeforcesMonitorProblemResult(
    val problemIndex: String,
    val points: Double,
    val type: CodeforcesProblemStatus
)

@kotlinx.serialization.Serializable
internal data class CodeforcesMonitorSubmissionInfo(
    val testset: CodeforcesTestset,
    val verdict: CodeforcesProblemVerdict
) {
    constructor(submission: CodeforcesSubmission): this(
        testset = submission.testset,
        verdict = submission.verdict
    )

    fun isPreliminary(): Boolean {
        if (verdict == CodeforcesProblemVerdict.WAITING) return true
        if (verdict == CodeforcesProblemVerdict.TESTING) return true
        if (testset == CodeforcesTestset.PRETESTS) {
            return verdict == CodeforcesProblemVerdict.OK
        }
        return false
    }

    fun isFailedSystemTest(): Boolean =
        testset == CodeforcesTestset.TESTS && verdict.isResult() && verdict != CodeforcesProblemVerdict.OK
}

fun CodeforcesMonitorDataStore.flowOfContestData(): Flow<CodeforcesMonitorData?> =
    flowBy { prefs ->
        val contestId = prefs[contestId] ?: return@flowBy null
        val contest = prefs[contestInfo]?.copy(id = contestId) ?: return@flowBy null
        val phase = when (contest.phase) {
            CodeforcesContestPhase.CODING -> CodeforcesMonitorData.ContestPhase.Coding(contest.startTime + contest.duration)
            CodeforcesContestPhase.SYSTEM_TEST -> CodeforcesMonitorData.ContestPhase.SystemTesting(prefs[sysTestPercentage])
            else -> CodeforcesMonitorData.ContestPhase.Other(contest.phase)
        }
        val contestantRank = CodeforcesMonitorData.ContestRank(
            rank = prefs[contestantRank],
            participationType = prefs[participationType]
        )
        val problems = prefs[problemResults].map { problem ->
            val index = problem.problemIndex
            val result: CodeforcesMonitorData.ProblemResult = when {
                contest.phase.isSystemTestOrFinished() && problem.type == CodeforcesProblemStatus.PRELIMINARY
                    -> CodeforcesMonitorData.ProblemResult.Pending
                problem.points != 0.0
                    -> CodeforcesMonitorData.ProblemResult.Points(
                        points = problem.points,
                        isFinal = problem.type == CodeforcesProblemStatus.FINAL
                    )
                prefs[submissionsInfo][index]?.any { it.isFailedSystemTest() } == true
                    -> CodeforcesMonitorData.ProblemResult.FailedSystemTest
                else
                    -> CodeforcesMonitorData.ProblemResult.Empty
            }
            index to result
        }
        CodeforcesMonitorData(
            contestInfo = contest,
            contestPhase = phase,
            contestantRank = contestantRank,
            problems = problems
        )
    }