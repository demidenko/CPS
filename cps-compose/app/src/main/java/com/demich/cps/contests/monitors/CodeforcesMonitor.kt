package com.demich.cps.contests.monitors

import com.demich.cps.platforms.api.*
import com.demich.cps.platforms.utils.CodeforcesUtils
import com.demich.cps.utils.getCurrentTime
import com.demich.datastore_itemized.add
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

suspend fun CodeforcesMonitorDataStore.launchIn(
    scope: CoroutineScope,
    onRatingChange: (CodeforcesRatingChange) -> Unit,
    onSubmissionFinalResult: (CodeforcesSubmission) -> Unit
) {
    val contestId = contestId() ?: return

    val ratingChangeWaiter = RatingChangeWaiter(contestId, handle(), onRatingChange)

    val mainJob = scope.launchWhileActive {
        val prevParticipationType = participationType()

        getStandingsData(contestId)

        if (isBecomeContestant(old = prevParticipationType, new = participationType())) {
            return@launchWhileActive Duration.ZERO
        }

        val contestInfo = contestInfo()

        if (needCheckSubmissions(contestInfo, participationType())) {
            val problemResults = problemResults()
            val info = submissionsInfo()
            if (problemResults.any { needCheckSubmissions(it, info) }) {
                getSubmissions(
                    contestId = contestId,
                    handle = handle()
                )?.let { submissions ->
                    val notified = notifiedSubmissionsIds()
                    submissions.filter {
                           it.testset == CodeforcesTestset.TESTS
                        && it.verdict.isResult()
                        && it.id !in notified
                    }.forEach {
                        onSubmissionFinalResult(it)
                        notifiedSubmissionsIds.add(it.id)
                    }
                    submissionsInfo.update { problemResults.makeMapWith(submissions) }
                }
            }
        }

        getDelay(
            contestPhase = contestInfo.phase,
            ratingChangeWaiter = ratingChangeWaiter
        )
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
                contestInfo.update { it.copy(phase = CodeforcesContestPhase.BEFORE) }
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

    val row = standings.rows.find { row -> row.party.participantType.contestParticipant() }
    val results = row?.problemResults ?: emptyList()
    problemResults(standings.problems.mapIndexed { index, problem ->
        val result = results.getOrNull(index)
        CodeforcesMonitorProblemResult(
            problemIndex = problem.index,
            points = result?.points ?: 0.0,
            type = result?.type ?: CodeforcesProblemStatus.FINAL
        )
    })

    row?.run {
        party.participantType.let {
            val old = participationType()
            participationType(it)
            if (isBecomeContestant(old = old, new = it)) return
        }
        contestantRank(rank)
    }
}

private suspend fun getDelay(
    contestPhase: CodeforcesContestPhase,
    ratingChangeWaiter: RatingChangeWaiter
): Duration {
    return when (contestPhase) {
        CodeforcesContestPhase.CODING -> 10.seconds
        CodeforcesContestPhase.PENDING_SYSTEM_TEST -> 15.seconds
        CodeforcesContestPhase.SYSTEM_TEST -> 3.seconds
        CodeforcesContestPhase.FINISHED -> ratingChangeWaiter.getDelayOnFinished()
        else -> 5.seconds
    }
}


private class RatingChangeWaiter(
    val contestId: Int,
    val handle: String,
    val onRatingChange: (CodeforcesRatingChange) -> Unit
) {
    private val waitingStartTime by lazy { getCurrentTime() }

    suspend fun getDelayOnFinished(): Duration {
        if (isRatingChangeDone()) return Duration.INFINITE
        val waitingTime = getCurrentTime() - waitingStartTime
        return when {
            waitingTime < 30.minutes -> 10.seconds
            waitingTime < 1.hours -> 30.seconds
            waitingTime < 4.hours -> 1.minutes
            else -> Duration.INFINITE
        }
    }

    private suspend fun isRatingChangeDone(): Boolean {
        CodeforcesApi.runCatching {
            getContestRatingChanges(contestId)
        }.getOrElse {
            if (it is CodeforcesAPIErrorResponse && it.isContestRatingUnavailable()) {
                //unrated contest
                return true
            }
            return false
        }.let { ratingChanges ->
            if (ratingChanges.isEmpty()) return false
            ratingChanges.find { it.handle == handle }?.let(onRatingChange)
            return true
        }
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


private fun needCheckSubmissions(
    contestInfo: CodeforcesContest,
    participationType: CodeforcesParticipationType
): Boolean {
    if (!participationType.contestParticipant()) return false
    if (contestInfo.type == CodeforcesContestType.ICPC) return false
    return contestInfo.phase.isSystemTestOrFinished()
}

private fun needCheckSubmissions(
    problemResult: CodeforcesMonitorProblemResult,
    submissionsInfo: Map<String, List<CodeforcesMonitorSubmissionInfo>>
): Boolean {
    if (problemResult.type != CodeforcesProblemStatus.FINAL) return false
    val list = submissionsInfo[problemResult.problemIndex] ?: return true
    return list.any { it.isPreliminary() }
}

private suspend fun getSubmissions(contestId: Int, handle: String) =
    CodeforcesApi.runCatching {
        getContestSubmissions(contestId = contestId, handle = handle)
    }.map { submissions ->
        submissions.filter {
               it.author.participantType.contestParticipant()
            && it.verdict != CodeforcesProblemVerdict.SKIPPED
        }
    }.getOrNull()

private fun List<CodeforcesMonitorProblemResult>.makeMapWith(submissions: List<CodeforcesSubmission>) =
    submissions.groupBy(
        keySelector = { it.problem.index },
        valueTransform = { CodeforcesMonitorSubmissionInfo(it) }
    ).let { grouped ->
        associateBy(
            keySelector = { it.problemIndex },
            valueTransform = { grouped[it.problemIndex] ?: emptyList() }
        ).mapValues { it.value.distinct() }
    }