package com.demich.cps.contests.monitors

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.demich.cps.utils.codeforces.*
import com.demich.cps.utils.jsonCPS
import com.demich.datastore_itemized.ItemizedDataStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

suspend fun CodeforcesMonitorDataStore.launchIn(scope: CoroutineScope) {
    val contestId = contestId() ?: return

    val mainJob = scope.launchWhileActive {
        val prevParticipationType = participationType()

        getStandingsData(contestId)

        if (prevParticipationType != CodeforcesParticipationType.CONTESTANT && participationType() == CodeforcesParticipationType.CONTESTANT) {
            return@launchWhileActive Duration.ZERO
        }

        val currentPhase = contestInfo().phase
        //TODO submissions result check

        getDelay(currentPhase, participationType())
    }

    val percentageJob = contestInfo.flow.map { it.phase }
        .collectSystemTestPercentage(
            contestId = contestId,
            scope = scope,
            delay = 5.seconds
        ) {
            sysTestPercentage(it)
        }

    this.contestId.flow
        .takeWhile { it == contestId }
        .onCompletion {
            percentageJob.cancel()
            mainJob.cancel()
        }
        .launchIn(scope)
}

private suspend fun CodeforcesMonitorDataStore.getStandingsData(contestId: Int) {
    CodeforcesApi.runCatching {
        getContestStandings(
            contestId = contestId,
            handle = handle(),
            includeUnofficial = participationType() != CodeforcesParticipationType.CONTESTANT
        )
    }.onFailure { e ->
        lastRequest(false)
        if (e is CodeforcesAPIErrorResponse) {
            if (e.isContestNotStarted(contestId)) {
                contestInfo.updateValue { it.copy(phase = CodeforcesContestPhase.BEFORE) }
            }
        }
    }.onSuccess { standings ->
        lastRequest(true)
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

private fun Flow<CodeforcesContestPhase>.collectSystemTestPercentage(
    contestId: Int,
    scope: CoroutineScope,
    delay: Duration,
    onSetPercentage: suspend (Int) -> Unit
): Job {
    var job: Job? = null
    return distinctUntilChanged().onEach { phase ->
        if (phase == CodeforcesContestPhase.SYSTEM_TEST) {
            job = scope.launchWhileActive {
                CodeforcesUtils.getContestSystemTestingPercentage(contestId)?.let {
                    if (isActive) onSetPercentage(it)
                }
                delay
            }
        } else {
            job?.cancel()
        }
    }.launchIn(scope)
}


private fun CoroutineScope.launchWhileActive(block: suspend CoroutineScope.() -> Duration) =
    launch {
        while (isActive) {
            val delayNext = block()
            if (delayNext == Duration.INFINITE) break
            if (delayNext > Duration.ZERO) delay(delayNext)
        }
    }



class CodeforcesMonitorDataStore(context: Context): ItemizedDataStore(context.cf_monitor_dataStore) {
    companion object {
        private val Context.cf_monitor_dataStore by preferencesDataStore(name = "cf_monitor")
    }

    val contestId = itemIntNullable(name = "contest_id")
    val handle = itemString(name = "handle", defaultValue = "")

    val lastRequest = jsonCPS.item<Boolean?>(name = "last_request", defaultValue = null)

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
            lastRequest,
            contestInfo,
            problemIndices,
            participationType,
            contestantRank,
            problemResults,
            sysTestPercentage
        ))
}