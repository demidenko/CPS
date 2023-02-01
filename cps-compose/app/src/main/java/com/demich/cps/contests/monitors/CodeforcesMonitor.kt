package com.demich.cps.contests.monitors

import com.demich.cps.utils.codeforces.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

suspend fun CodeforcesMonitorDataStore.launchIn(scope: CoroutineScope) {
    val contestId = contestId() ?: return

    val mainJob = scope.launchWhileActive {
        val prevParticipationType = participationType()

        getStandingsData(contestId)

        if (isBecomeContestant(old = prevParticipationType, new = participationType())) {
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
        applyStandings(standings)
    }
}

private fun isBecomeContestant(
    old: CodeforcesParticipationType,
    new: CodeforcesParticipationType
): Boolean {
    return new == CodeforcesParticipationType.CONTESTANT && old != CodeforcesParticipationType.CONTESTANT
}

private suspend fun CodeforcesMonitorDataStore.applyStandings(
    standings: CodeforcesContestStandings
) {
    contestInfo(standings.contest)

    val row = standings.rows.find { row -> row.party.participantType.participatedInContest() }
    val results = row?.problemResults ?: emptyList()
    problemResults(standings.problems.mapIndexed { index, problem ->
        val result = results.getOrNull(index)
        CodeforcesMonitorProblemResult(
            problemIndex = problem.index,
            points = result?.points ?: 0.0,
            type = result?.type ?: CodeforcesProblemStatus.FINAL
        )
    })

    if (row == null) return

    row.party.participantType.let {
        if (isBecomeContestant(old = participationType(), new = it)) {
            participationType(it)
            return
        }
    }
    participationType(row.party.participantType)
    contestantRank(row.rank)
}

private fun getDelay(
    contestPhase: CodeforcesContestPhase,
    participationType: CodeforcesParticipationType
): Duration {
    when (contestPhase) {
        CodeforcesContestPhase.CODING -> return 3.seconds
        CodeforcesContestPhase.SYSTEM_TEST -> return 3.seconds
        CodeforcesContestPhase.PENDING_SYSTEM_TEST -> return 15.seconds
        CodeforcesContestPhase.UNDEFINED -> return 5.seconds
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


@kotlinx.serialization.Serializable
data class CodeforcesMonitorProblemResult(
    val problemIndex: String,
    val points: Double,
    val type: CodeforcesProblemStatus
) {
    fun pointsToNiceString() =
        if (points != 0.0) points.toString().removeSuffix(".0") else ""
}