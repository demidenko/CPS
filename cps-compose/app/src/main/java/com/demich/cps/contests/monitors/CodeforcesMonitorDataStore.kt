package com.demich.cps.contests.monitors

import android.content.Context
import com.demich.cps.platforms.api.codeforces.models.CodeforcesContest
import com.demich.cps.platforms.api.codeforces.models.CodeforcesParticipationType
import com.demich.cps.platforms.api.codeforces.models.CodeforcesProblemStatus
import com.demich.cps.platforms.api.codeforces.models.CodeforcesProblemVerdict
import com.demich.cps.platforms.api.codeforces.models.CodeforcesSubmission
import com.demich.cps.platforms.api.codeforces.models.CodeforcesTestset
import com.demich.cps.platforms.api.codeforces.models.endTime
import com.demich.cps.platforms.api.codeforces.models.isResult
import com.demich.cps.platforms.api.codeforces.models.isSystemTestOrFinished
import com.demich.cps.utils.jsonCPS
import com.demich.datastore_itemized.DataStoreSnapshot
import com.demich.datastore_itemized.ItemizedDataStore
import com.demich.datastore_itemized.dataStoreWrapper
import com.demich.datastore_itemized.flowOf
import com.demich.datastore_itemized.value
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

class CodeforcesMonitorDataStore(context: Context): ItemizedDataStore(context.cf_monitor_dataStore) {
    companion object {
        private val Context.cf_monitor_dataStore by dataStoreWrapper(name = "cf_monitor")
    }

    val args = jsonCPS.itemNullable<CodeforcesMonitorArgs>(name = "args")

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
data class CodeforcesMonitorArgs(
    val contestId: Int,
    val handle: String
)

@Serializable
internal data class CodeforcesMonitorProblemResult(
    val problemIndex: String,
    val points: Double,
    val status: CodeforcesProblemStatus
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
        if (testset == PRETESTS) {
            return verdict == OK
        }
        return false
    }

    fun isFailedSystemTest(): Boolean =
        testset == TESTS && verdict.isResult() && verdict != OK
}

context(scope: DataStoreSnapshot)
private fun CodeforcesMonitorDataStore.contestInfoChecked(): CodeforcesContest? {
    // args can be changed before contestInfo reset
    val contestId = args.value?.contestId ?: return null
    val contest = contestInfo.value ?: return null
    return when {
        contest.id != contestId -> null
        contest.phase == UNDEFINED -> null
        else -> contest
    }
}

context(scope: DataStoreSnapshot)
private fun CodeforcesMonitorDataStore.contestData(): CodeforcesMonitorData? {
    val contest = contestInfoChecked() ?: return null

    val phase = when (contest.phase) {
        CODING ->
            CodeforcesMonitorData.ContestPhase.Coding(endTime = contest.endTime)
        SYSTEM_TEST ->
            CodeforcesMonitorData.ContestPhase.SystemTesting(percentage = sysTestPercentage.value)
        else ->
            CodeforcesMonitorData.ContestPhase.Other(phase = contest.phase)
    }

    val contestantRank = CodeforcesMonitorData.ContestRank(
        rank = contestantRank.value,
        participationType = participationType.value
    )

    val submissionsInfo = submissionsInfo.value
    val problems = problemResults.value.map { result ->
        val index = result.problemIndex
        CodeforcesMonitorData.ProblemInfo(
            name = index,
            result = when {
                contest.phase.isSystemTestOrFinished() && result.status == PRELIMINARY ->
                    CodeforcesMonitorData.ProblemResult.Pending
                result.points != 0.0 ->
                    CodeforcesMonitorData.ProblemResult.Points(
                        points = result.points,
                        isFinal = result.status == FINAL
                    )
                submissionsInfo[index]?.any { it.isFailedSystemTest() } == true ->
                    CodeforcesMonitorData.ProblemResult.FailedSystemTest
                else ->
                    CodeforcesMonitorData.ProblemResult.Empty
            }
        )
    }

    return CodeforcesMonitorData(
        contestInfo = contest,
        contestPhase = phase,
        contestantRank = contestantRank,
        problems = problems
    )
}

fun CodeforcesMonitorDataStore.flowOfContestData(): Flow<CodeforcesMonitorData?> =
    flowOf {
        contestData()
    }

fun CodeforcesMonitorDataStore.flowOfContestId(): Flow<Int?> =
    flowOf {
        contestInfoChecked()?.id
    }