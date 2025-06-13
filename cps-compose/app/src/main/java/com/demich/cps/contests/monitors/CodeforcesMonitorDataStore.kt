package com.demich.cps.contests.monitors

import android.content.Context
import com.demich.cps.platforms.api.codeforces.models.CodeforcesContest
import com.demich.cps.platforms.api.codeforces.models.CodeforcesContestPhase
import com.demich.cps.platforms.api.codeforces.models.CodeforcesContestType
import com.demich.cps.platforms.api.codeforces.models.CodeforcesParticipationType
import com.demich.cps.platforms.api.codeforces.models.CodeforcesProblemStatus
import com.demich.cps.platforms.api.codeforces.models.CodeforcesProblemVerdict
import com.demich.cps.platforms.api.codeforces.models.CodeforcesSubmission
import com.demich.cps.platforms.api.codeforces.models.CodeforcesTestset
import com.demich.cps.utils.jsonCPS
import com.demich.datastore_itemized.ItemizedDataStore
import com.demich.datastore_itemized.dataStoreWrapper
import com.demich.datastore_itemized.flowOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.time.Duration

class CodeforcesMonitorDataStore(context: Context): ItemizedDataStore(context.cf_monitor_dataStore) {
    companion object {
        private val Context.cf_monitor_dataStore by dataStoreWrapper(name = "cf_monitor")
    }

    internal val args = jsonCPS.item<CodeforcesMonitorArgs?>(name = "args", defaultValue = null)

    val lastRequest = jsonCPS.item<Boolean?>(name = "last_request", defaultValue = null)

    internal val contestInfo = jsonCPS.item(name = "contest_info") {
        CodeforcesContest(
            id = -1,
            name = "",
            phase = CodeforcesContestPhase.UNDEFINED,
            type = CodeforcesContestType.UNDEFINED,
            duration = Duration.ZERO,
            startTime = Instant.DISTANT_PAST
        )
    }

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
    flowOf { prefs ->
        val contest = prefs[contestInfo]
        prefs[args].let { if (it?.contestId != contest.id) return@flowOf null }

        val phase = when (contest.phase) {
            CodeforcesContestPhase.CODING -> {
                CodeforcesMonitorData.ContestPhase.Coding(endTime = contest.startTime + contest.duration)
            }
            CodeforcesContestPhase.SYSTEM_TEST -> {
                CodeforcesMonitorData.ContestPhase.SystemTesting(percentage = prefs[sysTestPercentage])
            }
            else -> {
                CodeforcesMonitorData.ContestPhase.Other(phase = contest.phase)
            }
        }

        val contestantRank = CodeforcesMonitorData.ContestRank(
            rank = prefs[contestantRank],
            participationType = prefs[participationType]
        )

        val problems = prefs[problemResults].map { problem ->
            val index = problem.problemIndex
            CodeforcesMonitorData.ProblemInfo(
                name = index,
                result = when {
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
            )
        }

        CodeforcesMonitorData(
            contestInfo = contest,
            contestPhase = phase,
            contestantRank = contestantRank,
            problems = problems
        )
    }

fun CodeforcesMonitorDataStore.flowOfContestId(): Flow<Int?> =
    flowOf { prefs ->
        prefs[args]?.contestId?.takeIf {
            prefs[contestInfo].phase != CodeforcesContestPhase.UNDEFINED
        }
    }.distinctUntilChanged()