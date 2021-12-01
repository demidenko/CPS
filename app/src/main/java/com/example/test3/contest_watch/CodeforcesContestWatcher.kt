package com.example.test3.contest_watch

import com.example.test3.utils.*
import kotlinx.coroutines.*
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class CodeforcesContestWatcher(val handle: String, val contestID: Int): CodeforcesContestWatchListener {

    suspend fun start(){
        val contestInfo = ChangingValue(
            CodeforcesContest(
                id = -1,
                name = "",
                phase = CodeforcesContestPhase.UNDEFINED,
                type = CodeforcesContestType.UNDEFINED,
                duration = Duration.ZERO,
                startTime = Instant.DISTANT_PAST,
                relativeTimeSeconds = 0
            ),
            isEquals = { c1, c2 ->
                c1.phase == c2.phase
                        && c1.startTime == c2.startTime
                        && c1.duration == c2.duration
                        && c1.name == c2.name
                        && c1.type == c2.type
            }
        )
        val contestantRank = ChangingValue(-1)
        val contestantPoints = ChangingValue(0.0)
        val participationType = ChangingValue(CodeforcesParticipationType.NOT_PARTICIPATED)
        var problemNames: List<String>? = null
        var problemsResults: List<ChangingValue<CodeforcesProblemResult>> = emptyList()
        val sysTestPercentage = ChangingValue(-1)
        val testedSubmissions = mutableSetOf<Long>()

        while (true) {
            CodeforcesAPI.getContestStandings(
                contestID,
                handle,
                participationType.value!=CodeforcesParticipationType.CONTESTANT
            )?.run {
                if(status == CodeforcesAPIStatus.FAILED){
                    if(isContestNotStarted(contestID)) contestInfo.value = contestInfo.value.copy(phase = CodeforcesContestPhase.BEFORE)
                    return@run
                }
                result?.run {
                    if(problemNames == null) problemNames = problems.map { it.index }.also {
                        onSetProblemNames(it.toTypedArray())
                    }

                    contestInfo.value = contest

                    rows.find { row ->
                        row.party.participantType.participatedInContest()
                    }?.let { row ->
                        participationType.value = row.party.participantType
                        contestantRank.value = row.rank
                        contestantPoints.value = row.points
                        with(row.problemResults){
                            if (this.size != problemsResults.size) problemsResults = this.map { ChangingValue(it, true) }
                            else this.forEachIndexed { index, result -> problemsResults[index].value = result }
                        }
                    }
                }
            }


            if(contestInfo.isChanged()) onSetContestInfo(contestInfo.value)

            if(contestInfo.value.phase == CodeforcesContestPhase.SYSTEM_TEST){
                //get progress of testing (0% ... 100%)
                delay(1000)
                CodeforcesAPI.getContestSystemTestingPercentage(contestID)?.let { percentage ->
                    sysTestPercentage.value = percentage
                    if(sysTestPercentage.isChanged()) onSetSysTestProgress(sysTestPercentage.value)
                }
            }


            if(participationType.isChanged()){
                onSetParticipationType(participationType.value)
                if(participationType.value == CodeforcesParticipationType.CONTESTANT){
                    contestantPoints.value = 0.0
                    contestantRank.value = -1
                    problemsResults = emptyList()
                    continue
                }
            }

            if(contestantRank.value != -1){
                if(contestantRank.isChanged()) onSetContestantRank(contestantRank.value)
                if(contestantPoints.isChanged()) onSetContestantPoints(contestantPoints.value)

                problemsResults.forEachIndexed { index, changingValue ->
                    if(changingValue.isChanged()){
                        val result = changingValue.value
                        val problemName = problemNames!![index]
                        onSetProblemResult(problemName, result)
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
                CodeforcesAPI.getContestSubmissions(contestID, handle)?.result
                    ?.filter { submission ->
                            submission.id !in testedSubmissions
                            &&
                            submission.author.participantType.participatedInContest()
                            &&
                            submission.testset == CodeforcesTestset.TESTS
                            &&
                            submission.verdict != CodeforcesProblemVerdict.WAITING
                            &&
                            submission.verdict != CodeforcesProblemVerdict.TESTING
                            &&
                            submission.verdict != CodeforcesProblemVerdict.SKIPPED
                    }?.forEach { submission ->
                        testedSubmissions.add(submission.id)
                        onSetProblemSystestResult(submission)
                    }
            }



            commit()

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

    private suspend fun checkRatingChanges(
        participationType: CodeforcesParticipationType
    ): Boolean {
        if(participationType != CodeforcesParticipationType.CONTESTANT) return true

        val response = CodeforcesAPI.getContestRatingChanges(contestID) ?: return false
        if(response.status == CodeforcesAPIStatus.FAILED){
            if(response.isContestRatingUnavailable()) return true
            return false
        }

        val change = response.result?.findLast { it.handle == handle } ?: return false

        onRatingChange(change)

        return true
    }

    private val listeners = mutableListOf<CodeforcesContestWatchListener>()

    fun addCodeforcesContestWatchListener(listener: CodeforcesContestWatchListener) = listeners.add(listener)

    override fun onSetContestInfo(contest: CodeforcesContest) {
        listeners.forEach { l -> l.onSetContestInfo(contest) }
    }

    override fun onSetProblemNames(problemNames: Array<String>) {
        listeners.forEach { l -> l.onSetProblemNames(problemNames) }
    }

    override fun onSetSysTestProgress(percents: Int) {
        listeners.forEach { l -> l.onSetSysTestProgress(percents) }
    }

    override fun onSetContestantRank(rank: Int) {
        listeners.forEach { l -> l.onSetContestantRank(rank) }
    }

    override fun onSetContestantPoints(points: Double) {
        listeners.forEach { l -> l.onSetContestantPoints(points) }
    }

    override fun onSetProblemResult(problemName: String, result: CodeforcesProblemResult) {
        listeners.forEach { l -> l.onSetProblemResult(problemName, result) }
    }

    override fun onSetProblemSystestResult(submission: CodeforcesSubmission) {
        listeners.forEach { l -> l.onSetProblemSystestResult(submission) }
    }

    override fun onSetParticipationType(type: CodeforcesParticipationType) {
        listeners.forEach { l -> l.onSetParticipationType(type) }
    }

    override suspend fun onRatingChange(ratingChange: CodeforcesRatingChange) {
        listeners.forEach { l -> l.onRatingChange(ratingChange) }
    }

    override fun commit() {
        listeners.forEach { l -> l.commit() }
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
    fun commit()
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