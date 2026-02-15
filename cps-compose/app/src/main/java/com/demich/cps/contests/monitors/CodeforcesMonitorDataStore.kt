package com.demich.cps.contests.monitors

import android.content.Context
import com.demich.cps.platforms.api.codeforces.models.CodeforcesContest
import com.demich.cps.platforms.api.codeforces.models.CodeforcesParticipationType
import com.demich.cps.platforms.api.codeforces.models.CodeforcesProblemStatus
import com.demich.cps.platforms.api.codeforces.models.CodeforcesProblemVerdict
import com.demich.cps.platforms.api.codeforces.models.CodeforcesSubmission
import com.demich.cps.platforms.api.codeforces.models.CodeforcesTestset
import com.demich.cps.utils.jsonCPS
import com.demich.datastore_itemized.ItemizedDataStore
import com.demich.datastore_itemized.dataStoreWrapper
import com.demich.datastore_itemized.flowOf
import com.demich.datastore_itemized.get
import com.demich.datastore_itemized.value
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

class CodeforcesMonitorDataStore(context: Context): ItemizedDataStore(context.cf_monitor_dataStore) {
    companion object {
        private val Context.cf_monitor_dataStore by dataStoreWrapper(name = "cf_monitor")
    }

    internal val args = jsonCPS.itemNullable<CodeforcesMonitorArgs>(name = "args")

    val lastRequest = jsonCPS.itemNullable<Boolean>(name = "last_request")

    internal val contestInfo = jsonCPS.itemNullable<CodeforcesContest>(name = "contest_info")

    internal val participationType = itemEnum(name = "participation_type", defaultValue = CodeforcesParticipationType.NOT_PARTICIPATED)

    internal val contestantRank = itemIntNullable(name = "contestant_rank")

    internal val problemResults = jsonCPS.itemList<CodeforcesMonitorProblemResult>(name = "problem_results")

    internal val submissionsInfo = jsonCPS.itemMap<String, List<CodeforcesMonitorSubmissionInfo>>(name = "problems_submissions_info")

    internal val sysTestPercentage = itemIntNullable(name = "sys_test_percentage")

    internal val notifiedSubmissionsIds = jsonCPS.itemSet<Long>(name = "submissions_notified")

    suspend fun reset() = resetAll()
}

@Serializable
internal data class CodeforcesMonitorArgs(
    val contestId: Int,
    val handle: String
)

@Serializable
internal data class CodeforcesMonitorProblemResult(
    val problemIndex: String,
    val points: Double,
    val type: CodeforcesProblemStatus
)

@Serializable
internal data class CodeforcesMonitorSubmissionInfo(
    val testset: CodeforcesTestset,
    val verdict: CodeforcesProblemVerdict
) {
    constructor(submission: CodeforcesSubmission): this(
        testset = submission.testset,
        verdict = submission.verdict
    )

    fun isPreliminary(): Boolean {
        if (verdict == WAITING) return true
        if (verdict == TESTING) return true
        if (testset == CodeforcesTestset.PRETESTS) {
            return verdict == OK
        }
        return false
    }

    fun isFailedSystemTest(): Boolean =
        testset == CodeforcesTestset.TESTS && verdict.isResult() && verdict != OK
}

fun CodeforcesMonitorDataStore.flowOfContestData(): Flow<CodeforcesMonitorData?> =
    flowOf {
        val contest = contestInfo.value ?: return@flowOf null
        args.value.let { if (it?.contestId != contest.id) return@flowOf null }
        if (contest.phase == UNDEFINED) return@flowOf null

        val phase = when (contest.phase) {
            CODING -> {
                CodeforcesMonitorData.ContestPhase.Coding(endTime = contest.startTime + contest.duration)
            }
            SYSTEM_TEST -> {
                CodeforcesMonitorData.ContestPhase.SystemTesting(percentage = sysTestPercentage.value)
            }
            else -> {
                CodeforcesMonitorData.ContestPhase.Other(phase = contest.phase)
            }
        }

        val contestantRank = CodeforcesMonitorData.ContestRank(
            rank = contestantRank.value,
            participationType = participationType.value
        )

        val problems = problemResults.value.map { problem ->
            val index = problem.problemIndex
            CodeforcesMonitorData.ProblemInfo(
                name = index,
                result = when {
                    contest.phase.isSystemTestOrFinished() && problem.type == PRELIMINARY
                        -> CodeforcesMonitorData.ProblemResult.Pending
                    problem.points != 0.0
                        -> CodeforcesMonitorData.ProblemResult.Points(
                            points = problem.points,
                            isFinal = problem.type == FINAL
                        )
                    submissionsInfo[index]?.any { it.isFailedSystemTest() } == true
                        -> CodeforcesMonitorData.ProblemResult.FailedSystemTest
                    else
                        -> CodeforcesMonitorData.ProblemResult.Empty
                }
            )
        }

        CodeforcesMonitorData(
            contestInfo = contest,
            contestPhase = phase,
            contestantRank = contestantRank,
            problems = problems
        )
    }

// shortcut for flowOfContestData().map { it?.contestId }
fun CodeforcesMonitorDataStore.flowOfContestId(): Flow<Int?> =
    flowOf {
        val contestInfo = contestInfo.value
        val contestId = args.value?.contestId
        when {
            contestInfo == null -> null
            contestInfo.id != contestId -> null
            contestInfo.phase == UNDEFINED -> null
            else -> contestId
        }
    }