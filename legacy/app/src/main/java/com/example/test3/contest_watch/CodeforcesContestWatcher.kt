package com.example.test3.contest_watch

import com.example.test3.utils.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class CodeforcesContestWatcher(
    val handle: String,
    val contestID: Int,
    val listener: CodeforcesContestWatchListener
) {

    suspend fun startIn(scope: CoroutineScope) {
        val contestInfo = MutableStateFlow(
            CodeforcesContest(
                id = -1,
                name = "",
                phase = CodeforcesContestPhase.UNDEFINED,
                type = CodeforcesContestType.UNDEFINED,
                duration = Duration.ZERO,
                startTime = Instant.DISTANT_PAST,
                relativeTimeSeconds = 0
            )
        )
        val contestantRank = MutableStateFlow(-1)
        val contestantPoints = MutableStateFlow(0.0)
        val participationType = MutableStateFlow(CodeforcesParticipationType.NOT_PARTICIPATED)
        var problemNames: List<String>? = null
        var problemsResults: List<ChangingValue<CodeforcesProblemResult>> = emptyList()
        val sysTestPercentage = MutableStateFlow(-1)
        val testedSubmissions = mutableSetOf<Long>()

        contestInfo.distinctUntilChanged { c1, c2 ->
            c1.phase == c2.phase
                    && c1.startTime == c2.startTime
                    && c1.duration == c2.duration
                    && c1.name == c2.name
                    && c1.type == c2.type
            }
            .onEach { listener.onSetContestInfo(it) }
            .launchIn(scope)

        sysTestPercentage.combine(contestInfo) { p, c -> Pair(p, c.phase) }
            .filter { it.second == CodeforcesContestPhase.SYSTEM_TEST }
            .onEach { listener.onSetSysTestProgress(it.first) }
            .launchIn(scope)

        participationType
            .onEach { listener.onSetParticipationType(it) }
            .launchIn(scope)

        contestantRank
            .onEach { listener.onSetContestantRank(it) }
            .launchIn(scope)

        contestantPoints
            .onEach { listener.onSetContestantPoints(it) }
            .launchIn(scope)

        while (true) {
            var restartFlag = false
            CodeforcesAPI.getContestStandings(
                contestID,
                handle,
                participationType.value!=CodeforcesParticipationType.CONTESTANT
            )?.run {
                if(status == CodeforcesAPIStatus.FAILED) {
                    if(isContestNotStarted(contestID)) contestInfo.update { it.copy(phase = CodeforcesContestPhase.BEFORE) }
                    return@run
                }
                result?.run {
                    if(problemNames == null) problemNames = problems.map { it.index }.also {
                        listener.onSetProblemNames(it.toTypedArray())
                    }

                    contestInfo.value = contest

                    rows.find { row ->
                        row.party.participantType.participatedInContest()
                    }?.let applyData@{ row ->
                        row.party.participantType.let {
                            val participationTypeBefore = participationType.value
                            participationType.value = it
                            if(participationTypeBefore != it && it == CodeforcesParticipationType.CONTESTANT) {
                                restartFlag = true
                                return@applyData
                            }
                        }
                        participationType.value = row.party.participantType
                        contestantRank.value = row.rank
                        contestantPoints.value = row.points
                        with(row.problemResults) {
                            if (this.size != problemsResults.size) problemsResults = this.map { ChangingValue(it, true) }
                            else this.forEachIndexed { index, result -> problemsResults[index].value = result }
                        }
                    }
                }
            }

            if(restartFlag) {
                contestantPoints.value = 0.0
                contestantRank.value = -1
                problemsResults = emptyList()
                continue
            }

            if(contestInfo.value.phase == CodeforcesContestPhase.SYSTEM_TEST) {
                delay(1000)
                CodeforcesAPI.getContestSystemTestingPercentage(contestID)?.let { percentage ->
                    sysTestPercentage.value = percentage
                }
            }

            if(contestantRank.value != -1) {
                problemsResults.forEachIndexed { index, changingValue ->
                    if(changingValue.isChanged()) {
                        val result = changingValue.value
                        val problemName = problemNames!![index]
                        listener.onSetProblemResult(problemName, result)
                    }
                }
            }

            if((contestInfo.value.phase in listOf(CodeforcesContestPhase.SYSTEM_TEST, CodeforcesContestPhase.FINISHED))
                && participationType.value.participatedInContest()
                //&& contestType.value == CodeforcesContestType.CF
                && problemsResults.any {
                    it.value.type == CodeforcesProblemStatus.PRELIMINARY || it.isChanged() && it.value.type == CodeforcesProblemStatus.FINAL
                }
            ){
                delay(1000)
                CodeforcesAPI.getContestSubmissions(contestID, handle)?.result?.run {
                    asSequence()
                        .filter { it.testset == CodeforcesTestset.TESTS }
                        .filter { it.verdict.isTested() }
                        .filter { it.author.participantType.participatedInContest() }
                        .filter { it.id !in testedSubmissions }
                        .forEach { submission ->
                            testedSubmissions.add(submission.id)
                            listener.onSetProblemSystestResult(submission)
                        }
                }
            }


            when(val delayTime = getDelay(contestInfo.value.phase, participationType.value)) {
                Duration.ZERO -> {
                    return
                    //delay(60_000)
                }
                else -> delay(delayTime.inWholeMilliseconds)
            }
        }
    }

    private var ratingChangeWaitingStartTime = Instant.DISTANT_PAST
    private suspend fun getDelay(contestPhase: CodeforcesContestPhase, participationType: CodeforcesParticipationType): Duration {
        when(contestPhase) {
            CodeforcesContestPhase.CODING -> return 3.seconds
            CodeforcesContestPhase.SYSTEM_TEST -> return 3.seconds
            CodeforcesContestPhase.PENDING_SYSTEM_TEST -> return 15.seconds
            CodeforcesContestPhase.FINISHED -> {
                if(checkRatingChanges(participationType)) return Duration.ZERO

                val currentTime = getCurrentTime()
                if(ratingChangeWaitingStartTime == Instant.DISTANT_PAST) ratingChangeWaitingStartTime = currentTime

                val waiting = currentTime - ratingChangeWaitingStartTime
                return when {
                    waiting < 30.minutes -> 10.seconds
                    waiting < 1.hours -> 30.seconds
                    waiting < 4.hours -> 1.minutes
                    else -> Duration.ZERO
                }
            }
            else -> return 30.seconds
        }
    }

    private suspend fun checkRatingChanges(participationType: CodeforcesParticipationType): Boolean {
        if(participationType != CodeforcesParticipationType.CONTESTANT) return true

        val response = CodeforcesAPI.getContestRatingChanges(contestID) ?: return false
        if(response.status == CodeforcesAPIStatus.FAILED){
            if(response.isContestRatingUnavailable()) return true
            return false
        }

        val change = response.result?.findLast { it.handle == handle } ?: return false

        listener.onRatingChange(change)

        return true
    }
}


interface CodeforcesContestWatchListener {
    fun onSetContestInfo(contest: CodeforcesContest)
    fun onSetProblemNames(problemNames: Array<String>)
    fun onSetSysTestProgress(percents: Int)
    fun onSetContestantRank(rank: Int)
    fun onSetContestantPoints(points: Double)
    fun onSetProblemResult(problemName: String, result: CodeforcesProblemResult)
    fun onSetProblemSystestResult(submission: CodeforcesSubmission)
    fun onSetParticipationType(type: CodeforcesParticipationType)
    suspend fun onRatingChange(ratingChange: CodeforcesRatingChange)
}


class ChangingValue<T>(
    private var x: T,
    private var changingFlag: Boolean = false,
    private val isEquals: (T, T) -> Boolean = { a, b -> a == b }
) {

    fun isChanged(): Boolean = changingFlag

    var value: T
        get() = x
        set(newValue) {
            if(isEquals(newValue, x)) changingFlag = false
            else {
                changingFlag = true
                x = newValue
            }
        }
}