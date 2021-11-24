package com.example.test3.contest_watch

import com.example.test3.utils.*
import kotlinx.coroutines.*
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class CodeforcesContestWatcher(val handle: String, val contestID: Int): CodeforcesContestWatchListener {

    suspend fun start(){
        val contestName = ChangingValue("")
        val contestType = ChangingValue(CodeforcesContestType.UNDEFINED)
        val phaseCodeforces = ChangingValue(CodeforcesContestPhase.UNDEFINED)
        val rank = ChangingValue(-1)
        val pointsTotal = ChangingValue(0.0)
        val duration = ChangingValue(Duration.ZERO)
        val startTime = ChangingValue(Instant.DISTANT_PAST)
        var problemNames: List<String>? = null
        val participationType = ChangingValue(CodeforcesParticipationType.NOT_PARTICIPATED)
        val sysTestPercentage = ChangingValue(-1)
        var problemsResults: List<ChangingValue<CodeforcesProblemResult>> = emptyList()
        val testedSubmissions = mutableSetOf<Long>()

        while (true) {
            var timeFromStart: Duration? = null
            CodeforcesAPI.getContestStandings(
                contestID,
                handle,
                participationType.value!=CodeforcesParticipationType.CONTESTANT
            )?.run {
                if(status == CodeforcesAPIStatus.FAILED){
                    if(isContestNotStarted(contestID)) phaseCodeforces.value = CodeforcesContestPhase.BEFORE
                    return@run
                }
                result?.run {
                    if(problemNames == null) problemNames = problems.map { it.index }.also {
                        onSetProblemNames(it.toTypedArray())
                    }

                    phaseCodeforces.value = contest.phase
                    timeFromStart = contest.relativeTimeSeconds.seconds
                    duration.value = contest.durationSeconds.seconds
                    startTime.value = contest.startTime
                    contestName.value = contest.name
                    contestType.value = contest.type

                    rows.find { row ->
                        row.party.participantType.participatedInContest()
                    }?.let { row ->
                        participationType.value = row.party.participantType
                        rank.value = row.rank
                        pointsTotal.value = row.points
                        with(row.problemResults){
                            if (this.size != problemsResults.size) problemsResults = this.map { ChangingValue(it, true) }
                            else this.forEachIndexed { index, result -> problemsResults[index].value = result }
                        }
                    }
                }
            }


            if(contestName.isChanged() || contestType.isChanged()) onSetContestNameAndType(contestName.value, contestType.value)
            if(phaseCodeforces.isChanged()) onSetContestPhase(phaseCodeforces.value)

            if(phaseCodeforces.value == CodeforcesContestPhase.CODING && (duration.isChanged() || startTime.isChanged())) timeFromStart?.let {
                val remainingTime = duration.value - it
                onSetRemainingTime(remainingTime)
            }

            if(phaseCodeforces.value == CodeforcesContestPhase.SYSTEM_TEST){
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
                    pointsTotal.value = 0.0
                    rank.value = -1
                    problemsResults = emptyList()
                    continue
                }
            }

            if(rank.value != -1){
                if(rank.isChanged()) onSetContestantRank(rank.value)
                if(pointsTotal.isChanged()) onSetContestantPoints(pointsTotal.value)

                problemsResults.forEachIndexed { index, changingValue ->
                    if(changingValue.isChanged()){
                        val result = changingValue.value
                        val problemName = problemNames!![index]
                        onSetProblemResult(problemName, result)
                    }
                }
            }

            if((phaseCodeforces.value == CodeforcesContestPhase.SYSTEM_TEST || phaseCodeforces.value == CodeforcesContestPhase.FINISHED)
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

            when(val delayTime = getDelay(phaseCodeforces.value, participationType.value)) {
                Duration.ZERO -> {
                    return
                    //delay(60_000)
                }
                else -> delay(delayTime.inWholeMilliseconds)
            }
        }
    }

    private var ratingChangeWaitingStartTimeMillis = 0L
    private suspend fun getDelay(contestPhase: CodeforcesContestPhase, participationType: CodeforcesParticipationType): Duration {
        when(contestPhase){
            CodeforcesContestPhase.CODING -> return 3.seconds
            CodeforcesContestPhase.SYSTEM_TEST -> return 3.seconds
            CodeforcesContestPhase.PENDING_SYSTEM_TEST -> return 15.seconds
            CodeforcesContestPhase.FINISHED -> {
                if(checkRatingChanges(participationType)) return Duration.ZERO

                val currentTimeMillis = System.currentTimeMillis()
                if(ratingChangeWaitingStartTimeMillis == 0L) ratingChangeWaitingStartTimeMillis = currentTimeMillis

                val hoursWaiting = (currentTimeMillis - ratingChangeWaitingStartTimeMillis).milliseconds.inWholeHours
                return when {
                    hoursWaiting <= 0 -> 10.seconds
                    hoursWaiting <= 1 -> 30.seconds
                    hoursWaiting <= 3 -> 60.seconds
                    else -> 0.seconds
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

    override fun onSetContestNameAndType(contestName: String, contestType: CodeforcesContestType) {
        listeners.forEach { l -> l.onSetContestNameAndType(contestName, contestType) }
    }

    override fun onSetProblemNames(problemNames: Array<String>) {
        listeners.forEach { l -> l.onSetProblemNames(problemNames) }
    }

    override fun onSetContestPhase(phase: CodeforcesContestPhase) {
        listeners.forEach { l -> l.onSetContestPhase(phase) }
    }

    override fun onSetRemainingTime(time: Duration) {
        listeners.forEach { l -> l.onSetRemainingTime(time) }
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
    fun onSetContestNameAndType(contestName: String, contestType: CodeforcesContestType)
    fun onSetProblemNames(problemNames: Array<String>)
    fun onSetContestPhase(phase: CodeforcesContestPhase)
    fun onSetRemainingTime(time: Duration)
    fun onSetSysTestProgress(percents: Int)
    fun onSetContestantRank(rank: Int)
    fun onSetContestantPoints(points: Double)
    fun onSetProblemResult(problemName: String, result: CodeforcesProblemResult)
    fun onSetProblemSystestResult(submission: CodeforcesSubmission)
    fun onSetParticipationType(type: CodeforcesParticipationType)
    suspend fun onRatingChange(ratingChange: CodeforcesRatingChange)
    fun commit()
}


class ChangingValue<T>(private var x: T, private var changingFlag: Boolean = false){

    fun isChanged(): Boolean = changingFlag

    var value: T
        get() = x
        set(newValue) {
            if(newValue != x){
                changingFlag = true
                x = newValue
            }else{
                changingFlag = false
            }
        }
}