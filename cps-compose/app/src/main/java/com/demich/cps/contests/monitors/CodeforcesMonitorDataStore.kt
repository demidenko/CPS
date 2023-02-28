package com.demich.cps.contests.monitors

import android.content.Context
import com.demich.cps.utils.codeforces.*
import com.demich.cps.utils.jsonCPS
import com.demich.datastore_itemized.ItemizedDataStore
import com.demich.datastore_itemized.dataStoreWrapper
import kotlinx.datetime.Instant
import kotlin.time.Duration

class CodeforcesMonitorDataStore(context: Context): ItemizedDataStore(context.cf_monitor_dataStore) {
    companion object {
        private val Context.cf_monitor_dataStore by dataStoreWrapper(name = "cf_monitor")
    }

    val contestId = itemIntNullable(name = "contest_id")
    val handle = itemString(name = "handle", defaultValue = "")

    val lastRequest = jsonCPS.item<Boolean?>(name = "last_request", defaultValue = null)

    val contestInfo = jsonCPS.item(
        name = "contest_info",
        defaultValue = CodeforcesContest(
            id = -1,
            name = "",
            phase = CodeforcesContestPhase.UNDEFINED,
            type = CodeforcesContestType.UNDEFINED,
            duration = Duration.ZERO,
            startTime = Instant.DISTANT_PAST,
            relativeTimeSeconds = 0
        )
    )

    val participationType = itemEnum(name = "participation_type", defaultValue = CodeforcesParticipationType.NOT_PARTICIPATED)
    val contestantRank = itemInt(name = "contestant_rank", defaultValue = -1)

    val problemResults = jsonCPS.item<List<CodeforcesMonitorProblemResult>>(
        name = "problem_results",
        defaultValue = emptyList()
    )

    val submissionsInfo = jsonCPS.item<Map<String, List<CodeforcesMonitorSubmissionInfo>>> (
        name = "problems_submissions_info",
        defaultValue = emptyMap()
    )

    val sysTestPercentage = itemIntNullable("sys_test_percentage")

    suspend fun reset() =
        resetItems(items = listOf(
            contestId,
            handle,
            lastRequest,
            contestInfo,
            participationType,
            contestantRank,
            problemResults,
            submissionsInfo,
            sysTestPercentage
        ))
}

@kotlinx.serialization.Serializable
data class CodeforcesMonitorProblemResult(
    val problemIndex: String,
    val points: Double,
    val type: CodeforcesProblemStatus
)

@kotlinx.serialization.Serializable
data class CodeforcesMonitorSubmissionInfo(
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
}