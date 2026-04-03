package com.demich.cps.contests.monitors

import com.demich.cps.platforms.api.codeforces.CodeforcesApi
import com.demich.cps.platforms.api.codeforces.CodeforcesApiContestNotFoundException
import com.demich.cps.platforms.api.codeforces.CodeforcesApiContestNotStartedException
import com.demich.cps.platforms.api.codeforces.CodeforcesPageContentProvider
import com.demich.cps.platforms.api.codeforces.getContestStandings
import com.demich.cps.platforms.api.codeforces.models.CodeforcesContest
import com.demich.cps.platforms.api.codeforces.models.CodeforcesContestPhase
import com.demich.cps.platforms.api.codeforces.models.CodeforcesContestStandings
import com.demich.cps.platforms.api.codeforces.models.CodeforcesParticipationType
import com.demich.cps.platforms.api.codeforces.models.CodeforcesRatingChange
import com.demich.cps.platforms.api.codeforces.models.CodeforcesSubmission
import com.demich.cps.platforms.api.codeforces.models.endTime
import com.demich.cps.platforms.api.codeforces.models.isContestant
import com.demich.cps.platforms.api.codeforces.models.isContestantType
import com.demich.cps.platforms.api.codeforces.models.isResult
import com.demich.cps.platforms.api.codeforces.models.isSystemTestOrFinished
import com.demich.cps.platforms.utils.codeforces.CodeforcesRatingChangesStatus
import com.demich.cps.platforms.utils.codeforces.getContestRatingChangesStatus
import com.demich.cps.platforms.utils.codeforces.getSysTestPercentageOrNull
import com.demich.cps.utils.launchWhileActive
import com.demich.datastore_itemized.DataStoreEditScope
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
import kotlin.time.Clock
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
        api = api,
        clock = Clock.System
    ) { ratingChanges ->
        ratingChanges.find { it.handle == handle }?.let(onRatingChange)
    }

    val mainJob = scope.launchWhileActive {
        api.updateStandingsData(
            contestId = contestId,
            handle = handle
        )

        ifNeedCheckSubmissions { problemResults ->
            api.getParticipantNonSkippedSubmissionsResult(
                contestId = contestId,
                handle = handle
            ).onSuccess { submissions ->
                val notified = notifiedSubmissionsIds()
                val newResultSubmissions = submissions.filter {
                    it.testset == TESTS
                    && it.verdict.isResult()
                    && it.id !in notified
                }
                newResultSubmissions.forEach { onSubmissionFinalResult(it) }
                edit {
                    notifiedSubmissionsIds.value += newResultSubmissions.map { it.id }
                    submissionsInfo.value = problemResults.makeMapWith(submissions)
                }
            }.onFailure { lastRequest.setValue(false) }
        }

        ratingChangeWaiter.getDelay(contest = contestInfo()).let { duration ->
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
    handle: String
) {
    runCatching {
        getContestStandings(
            contestId = contestId,
            handle = handle,
            participantTypes = listOf(CONTESTANT, OUT_OF_COMPETITION)
        )
    }.onFailure { e ->
        monitor.lastRequest.setValue(false)
        if (e is CodeforcesApiContestNotStartedException && e.contestId == contestId) {
            monitor.contestInfo.update { it?.copy(phase = BEFORE) }
        }
        if (e is CodeforcesApiContestNotFoundException && e.contestId == contestId) {
            monitor.reset()
        }
    }.onSuccess { standings ->
        val standings = standings.toStandingsData()
        monitor.edit {
            lastRequest.value = true
            saveStandings(standings = standings)
        }
    }
}

private class StandingsData(
    val contest: CodeforcesContest,
    val problemResults: List<CodeforcesMonitorProblemResult>,
    val rank: Int?,
    val participationType: CodeforcesParticipationType?
)

private fun CodeforcesContestStandings.findProperRow(): CodeforcesContestStandings.CodeforcesContestStandingsRow? {
    rows.forEach { if (it.party.participantType == CONTESTANT) return it }
    rows.forEach { if (it.party.participantType == OUT_OF_COMPETITION) return it }
    return null
}

private fun CodeforcesContestStandings.toStandingsData(): StandingsData {
    val row = findProperRow()
    val results = row?.problemResults

    val monitorResults = problems.mapIndexed { index, problem ->
        val result = results?.getOrNull(index)
        CodeforcesMonitorProblemResult(
            problemIndex = problem.index,
            points = result?.points ?: 0.0,
            status = result?.type ?: FINAL
        )
    }

    return StandingsData(
        contest = contest,
        problemResults = monitorResults,
        rank = row?.rank,
        participationType = row?.party?.participantType
    )
}

context(_: DataStoreEditScope)
private fun CodeforcesMonitorDataStore.saveStandings(
    standings: StandingsData
) {
    contestInfo.value = standings.contest
    problemResults.value = standings.problemResults
    participationType.value = standings.participationType
    contestantRank.value = standings.rank
}

private class RatingChangeWaiter(
    val contestId: Int,
    val api: CodeforcesApi,
    val clock: Clock,
    val onRatingChangeDone: (List<CodeforcesRatingChange>) -> Unit
) {
    suspend fun getDelayOnFinished(contestEnd: Instant): Duration {
        if (isRatingChangeDone()) return Duration.INFINITE
        val waitingTime = clock.now() - contestEnd
        return when {
            waitingTime < 30.minutes -> 10.seconds
            waitingTime < 1.hours -> 30.seconds
            waitingTime < 4.hours -> 1.minutes
            else -> 5.minutes
        }
    }

    private suspend fun isRatingChangeDone(): Boolean {
        val status = api.runCatching {
            getContestRatingChangesStatus(contestId = contestId)
        }.getOrElse {
            //TODO: save this failure?
            return false
        }

        return when (status) {
            CodeforcesRatingChangesStatus.Empty -> false
            CodeforcesRatingChangesStatus.Unavailable -> true
            is CodeforcesRatingChangesStatus.Done -> {
                onRatingChangeDone(status.ratingChanges)
                true
            }
        }
    }
}

private suspend fun RatingChangeWaiter.getDelay(contest: CodeforcesContest?): Duration {
    return when (contest?.phase) {
        CODING -> 10.seconds
        PENDING_SYSTEM_TEST -> 15.seconds
        SYSTEM_TEST -> 3.seconds
        FINISHED -> {
            getDelayOnFinished(contestEnd = contest.endTime)
        }
        else -> 5.seconds
    }
}

private fun Flow<CodeforcesContestPhase>.toSystemTestPercentageFlow(
    contestId: Int,
    delay: Duration,
    pageContentProvider: CodeforcesPageContentProvider
): Flow<Int> = distinctUntilChanged()
    .transformLatest { phase ->
        if (phase == SYSTEM_TEST) {
            while (true) {
                pageContentProvider.getSysTestPercentageOrNull(contestId = contestId)
                    ?.let { emit(it) }
                delay(duration = delay)
            }
        }
    }.distinctUntilChanged()


private fun CodeforcesContest.needCheckSubmissions(
    participationType: CodeforcesParticipationType?
): Boolean {
    if (type == ICPC) return false
    return phase.isSystemTestOrFinished() && (participationType != null && participationType.isContestantType())
}

private fun CodeforcesMonitorProblemResult.needCheckSubmissions(
    submissionsInfo: Map<String, List<CodeforcesSubmissionJudgeInfo>>
): Boolean {
    if (status != FINAL) return false
    val list = submissionsInfo[problemIndex] ?: return true
    return list.any { it.isPreliminary() }
}

private suspend inline fun CodeforcesMonitorDataStore.ifNeedCheckSubmissions(
    block: (List<CodeforcesMonitorProblemResult>) -> Unit
) = fromSnapshot {
    val contestInfo = contestInfo.value ?: return@fromSnapshot
    if (contestInfo.needCheckSubmissions(participationType = participationType.value)) {
        val problemResults = problemResults.value
        val info = submissionsInfo.value
        if (problemResults.any { it.needCheckSubmissions(info) }) block(problemResults)
    }
}

private suspend fun CodeforcesApi.getParticipantNonSkippedSubmissionsResult(
    contestId: Int,
    handle: String
): Result<List<CodeforcesSubmission>> =
    runCatching {
        getContestSubmissions(contestId = contestId, handle = handle)
    }.map { submissions ->
        submissions.filter {
               it.author.isContestant()
            && it.verdict != SKIPPED
        }
    }

private fun List<CodeforcesMonitorProblemResult>.makeMapWith(submissions: List<CodeforcesSubmission>) =
    submissions.groupBy(
        keySelector = { it.problem.index },
        valueTransform = { it.toJudgeInfo() }
    ).let { grouped ->
        associateBy(
            keySelector = { it.problemIndex },
            valueTransform = { grouped[it.problemIndex] ?: emptyList() }
        )
    }.mapValues {
        it.value.distinct()
            .filter { !it.isFailedPretests() } // do not save useless
    }