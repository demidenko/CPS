package com.demich.cps.contests.monitors

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.demich.cps.utils.codeforces.*
import com.demich.cps.utils.jsonCPS
import com.demich.datastore_itemized.ItemizedDataStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

suspend fun CodeforcesMonitorDataStore.runIn(scope: CoroutineScope) {
    collectSystemTestPercentage(scope = scope, delay = 5.seconds)
    while (true) {
        val contestId = contestId() ?: return
        val prevParticipationType = participationType()

        getStandingsData(contestId)

        if (prevParticipationType != CodeforcesParticipationType.CONTESTANT && participationType() == CodeforcesParticipationType.CONTESTANT) {
            continue
        }

        val currentPhase = contestInfo().phase
        //TODO submissions result check

        when (val delayTime = getDelay(currentPhase, participationType())) {
            Duration.INFINITE -> return
            else -> delay(delayTime)
        }
    }
}

private suspend fun CodeforcesMonitorDataStore.getStandingsData(contestId: Int) {
    CodeforcesApi.runCatching {
        getContestStandings(
            contestId = contestId,
            handle = handle(),
            includeUnofficial = participationType() != CodeforcesParticipationType.CONTESTANT
        )
    }.onFailure { e ->
        if (e is CodeforcesAPIErrorResponse) {
            if (e.isContestNotStarted(contestId)) {
                contestInfo.updateValue { it.copy(phase = CodeforcesContestPhase.BEFORE) }
            }
        }
    }.onSuccess { standings ->
        problemIndices(standings.problems.map { it.index })
        contestInfo(standings.contest)
        standings.rows.find { row -> row.party.participantType.participatedInContest() }
            ?.let { applyRow(it) }
    }
}

private suspend fun CodeforcesMonitorDataStore.applyRow(row: CodeforcesContestStandings.CodeforcesContestStandingsRow) {
    row.party.participantType.let {
        if (it == CodeforcesParticipationType.CONTESTANT && participationType() != it) {
            participationType(it)
            return
        }
    }
    participationType(row.party.participantType)
    contestantRank(row.rank)
    problemResults(row.problemResults)
}

private fun getDelay(
    contestPhase: CodeforcesContestPhase,
    participationType: CodeforcesParticipationType
): Duration {
    when (contestPhase) {
        CodeforcesContestPhase.CODING -> return 3.seconds
        CodeforcesContestPhase.SYSTEM_TEST -> return 3.seconds
        CodeforcesContestPhase.PENDING_SYSTEM_TEST -> return 15.seconds
        //CodeforcesContestPhase.FINISHED -> //TODO
        else -> return 30.seconds
    }
}

private fun CodeforcesMonitorDataStore.collectSystemTestPercentage(
    scope: CoroutineScope,
    delay: Duration
) {
    suspend fun percentageChecker() {
        while (scope.isActive) {
            contestId()?.let { contestId ->
                CodeforcesUtils.getContestSystemTestingPercentage(contestId)?.let {
                    sysTestPercentage(it)
                }
            }
            delay(delay)
        }
    }
    var job: Job? = null
    contestInfo.flow.map { it.phase }.distinctUntilChanged().onEach { phase ->
        if (phase == CodeforcesContestPhase.SYSTEM_TEST) {
            job = scope.launch { percentageChecker() }
        } else {
            job?.cancel()
        }
    }.launchIn(scope)
}

class CodeforcesMonitorDataStore(context: Context): ItemizedDataStore(context.cf_monitor_dataStore) {
    companion object {
        private val Context.cf_monitor_dataStore by preferencesDataStore(name = "cf_monitor")
    }

    val contestId = itemIntNullable(name = "contest_id")
    val handle = itemString(name = "handle", defaultValue = "")

    val contestInfo = jsonCPS.item(name = "contest_info", defaultValue = CodeforcesContest(
        id = -1,
        name = "",
        phase = CodeforcesContestPhase.UNDEFINED,
        type = CodeforcesContestType.UNDEFINED,
        duration = Duration.ZERO,
        startTime = Instant.DISTANT_PAST,
        relativeTimeSeconds = 0
    ))
    val problemIndices = jsonCPS.item(name = "problems", defaultValue = emptyList<String>())

    val participationType = itemEnum(name = "participation_type", defaultValue = CodeforcesParticipationType.NOT_PARTICIPATED)
    val contestantRank = itemInt(name = "contestant_rank", defaultValue = -1)
    val problemResults = jsonCPS.item(name = "problem_results", defaultValue = emptyList<CodeforcesProblemResult>())

    val sysTestPercentage = itemIntNullable("sys_test_percentage")

    suspend fun reset() =
        resetItems(items = listOf(
            contestId,
            handle,
            contestInfo,
            problemIndices,
            participationType,
            contestantRank,
            problemResults,
            sysTestPercentage
        ))
}