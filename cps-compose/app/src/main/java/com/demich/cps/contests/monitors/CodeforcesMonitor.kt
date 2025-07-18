package com.demich.cps.contests.monitors

import com.demich.cps.platforms.api.codeforces.CodeforcesApi
import com.demich.cps.platforms.api.codeforces.CodeforcesApiContestNotFoundException
import com.demich.cps.platforms.api.codeforces.CodeforcesApiContestNotStartedException
import com.demich.cps.platforms.api.codeforces.CodeforcesApiContestRatingUnavailableException
import com.demich.cps.platforms.api.codeforces.CodeforcesPageContentProvider
import com.demich.cps.platforms.api.codeforces.models.CodeforcesContest
import com.demich.cps.platforms.api.codeforces.models.CodeforcesContestPhase
import com.demich.cps.platforms.api.codeforces.models.CodeforcesContestStandings
import com.demich.cps.platforms.api.codeforces.models.CodeforcesContestType
import com.demich.cps.platforms.api.codeforces.models.CodeforcesParticipationType
import com.demich.cps.platforms.api.codeforces.models.CodeforcesProblemStatus
import com.demich.cps.platforms.api.codeforces.models.CodeforcesProblemVerdict
import com.demich.cps.platforms.api.codeforces.models.CodeforcesRatingChange
import com.demich.cps.platforms.api.codeforces.models.CodeforcesSubmission
import com.demich.cps.platforms.api.codeforces.models.CodeforcesTestset
import com.demich.cps.platforms.utils.codeforces.CodeforcesUtils
import com.demich.cps.utils.getCurrentTime
import com.demich.cps.utils.launchWhileActive
import com.demich.datastore_itemized.edit
import com.demich.datastore_itemized.fromSnapshot
import com.demich.datastore_itemized.value
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

suspend fun CodeforcesMonitorDataStore.launchIn(
    scope: CoroutineScope,
    api: CodeforcesApi,
    pageContentProvider: CodeforcesPageContentProvider,
    onRatingChange: (CodeforcesRatingChange) -> Unit,
    onSubmissionFinalResult: (CodeforcesSubmission) -> Unit,
    onCompletion: () -> Unit = {}
) {
    val (contestId: Int, handle: String) = args() ?: return

    val ratingChangeWaiter = RatingChangeWaiter(
        contestId = contestId,
        handle = handle,
        api = api,
        onRatingChange = onRatingChange
    )

    val mainJob = scope.launchWhileActive {
        api.updateStandingsData(
            contestId = contestId,
            handle = handle,
            onOfficialChanged = {
                return@launchWhileActive Duration.ZERO
            }
        )

        ifNeedCheckSubmissions { problemResults ->
            api.getSubmissionsOrNull(
                contestId = contestId,
                handle = handle
            )?.let { submissions ->
                val notified = notifiedSubmissionsIds()
                submissions.filter {
                    it.testset == CodeforcesTestset.TESTS
                    && it.verdict.isResult()
                    && it.id !in notified
                }.takeIf { it.isNotEmpty() }?.let { newSubmissions ->
                    newSubmissions.forEach { onSubmissionFinalResult(it) }
                    notifiedSubmissionsIds.edit { newSubmissions.forEach { add(it.id) } }
                }
                submissionsInfo.setValue(problemResults.makeMapWith(submissions))
            }
        }

        getDelay(
            contest = contestInfo(),
            ratingChangeWaiter = ratingChangeWaiter
        ).let { duration ->
            if (lastRequest() == false) duration.coerceAtMost(10.seconds)
            else duration
        }
    }

    val systestPercentageJob = scope.launch {
        contestInfo.asFlow().mapNotNull { it?.phase }
            .toSystemTestPercentageFlow(
                contestId = contestId,
                delay = 5.seconds,
                pageContentProvider = pageContentProvider
            )
            .collect {
                sysTestPercentage.setValue(it)
            }
    }

    args.asFlow()
        .takeWhile { it?.contestId == contestId }
        .onCompletion {
            systestPercentageJob.cancel()
            mainJob.cancel()
            onCompletion()
        }
        .launchIn(scope)
}

context(monitor: CodeforcesMonitorDataStore)
private suspend inline fun CodeforcesApi.updateStandingsData(
    contestId: Int,
    handle: String,
    onOfficialChanged: () -> Unit
) {
    runCatching {
        getContestStandings(
            contestId = contestId,
            handle = handle,
            includeUnofficial = !monitor.participationType().isOfficial()
        )
    }.onFailure { e ->
        monitor.lastRequest.setValue(false)
        if (e is CodeforcesApiContestNotStartedException && e.contestId == contestId) {
            monitor.contestInfo.update { it?.copy(phase = CodeforcesContestPhase.BEFORE) }
        }
        if (e is CodeforcesApiContestNotFoundException && e.contestId == contestId) {
            monitor.reset()
        }
    }.onSuccess { standings ->
        monitor.lastRequest.setValue(true)
        var officialChanged = false
        monitor.applyStandings(
            standings = standings,
            onOfficialChanged = { officialChanged = true }
        )
        if (officialChanged) {
            onOfficialChanged()
        }
    }
}

private fun CodeforcesParticipationType.isOfficial(): Boolean =
    this == CodeforcesParticipationType.CONTESTANT

private fun isOfficialChanged(
    old: CodeforcesParticipationType,
    new: CodeforcesParticipationType
): Boolean = new.isOfficial() != old.isOfficial()

private suspend fun CodeforcesMonitorDataStore.applyStandings(
    standings: CodeforcesContestStandings,
    onOfficialChanged: () -> Unit
) = edit { prefs ->
    prefs[contestInfo] = standings.contest

    val row = standings.rows.find { row -> row.party.isContestParticipant() }
    val results = row?.problemResults
    prefs[problemResults] = standings.problems.mapIndexed { index, problem ->
        val result = results?.getOrNull(index)
        CodeforcesMonitorProblemResult(
            problemIndex = problem.index,
            points = result?.points ?: 0.0,
            type = result?.type ?: CodeforcesProblemStatus.FINAL
        )
    }

    row?.run {
        party.participantType.let {
            val old = prefs[participationType]
            prefs[participationType] = it
            if (isOfficialChanged(old = old, new = it)) {
                onOfficialChanged()
                return@edit
            }
        }
        prefs[contestantRank] = rank
    }
}

private suspend fun getDelay(
    contest: CodeforcesContest?,
    ratingChangeWaiter: RatingChangeWaiter
): Duration {
    return when (contest?.phase) {
        CodeforcesContestPhase.CODING -> 10.seconds
        CodeforcesContestPhase.PENDING_SYSTEM_TEST -> 15.seconds
        CodeforcesContestPhase.SYSTEM_TEST -> 3.seconds
        CodeforcesContestPhase.FINISHED -> {
            ratingChangeWaiter.getDelayOnFinished(contestEnd = contest.startTime + contest.duration)
        }
        else -> 5.seconds
    }
}


private class RatingChangeWaiter(
    val contestId: Int,
    val handle: String,
    val api: CodeforcesApi,
    val onRatingChange: (CodeforcesRatingChange) -> Unit
) {
    suspend fun getDelayOnFinished(contestEnd: Instant): Duration {
        if (isRatingChangeDone()) return Duration.INFINITE
        val waitingTime = getCurrentTime() - contestEnd
        return when {
            waitingTime < 30.minutes -> 10.seconds
            waitingTime < 1.hours -> 30.seconds
            waitingTime < 4.hours -> 1.minutes
            else -> 5.minutes
        }
    }

    private suspend fun isRatingChangeDone(): Boolean {
        api.runCatching {
            getContestRatingChanges(contestId)
        }.getOrElse {
            //TODO: take this failure
            if (it is CodeforcesApiContestRatingUnavailableException) {
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


private fun Flow<CodeforcesContestPhase>.toSystemTestPercentageFlow(
    contestId: Int,
    delay: Duration,
    pageContentProvider: CodeforcesPageContentProvider
): Flow<Int> = distinctUntilChanged().transformLatest { phase ->
    if (phase == CodeforcesContestPhase.SYSTEM_TEST) {
        while (true) {
            pageContentProvider.runCatching { getContestPage(contestId) }
                .map { CodeforcesUtils.extractContestSystemTestingPercentageOrNull(it) }
                .onSuccess {
                    if (it != null) emit(it)
                }
            delay(duration = delay)
        }
    }
}


private fun needCheckSubmissions(
    contestInfo: CodeforcesContest,
    participationType: CodeforcesParticipationType
): Boolean {
    if (!participationType.isContestParticipant()) return false
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

private suspend inline fun CodeforcesMonitorDataStore.ifNeedCheckSubmissions(
    block: (List<CodeforcesMonitorProblemResult>) -> Unit
) = fromSnapshot {
    val contestInfo = contestInfo.value ?: return@fromSnapshot
    if (needCheckSubmissions(contestInfo = contestInfo, participationType = participationType.value)) {
        val problemResults = problemResults.value
        val info = submissionsInfo.value
        if (problemResults.any { needCheckSubmissions(it, info) }) block(problemResults)
    }
}

private suspend fun CodeforcesApi.getSubmissionsOrNull(
    contestId: Int,
    handle: String
): List<CodeforcesSubmission>? =
    runCatching {
        getContestSubmissions(contestId = contestId, handle = handle)
    }.map { submissions ->
        submissions.filter {
               it.author.isContestParticipant()
            && it.verdict != CodeforcesProblemVerdict.SKIPPED
        }
    }.getOrNull() //TODO: take this failure

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