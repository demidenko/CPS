package com.example.test3.contest_watch

import com.example.test3.utils.*
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import kotlinx.coroutines.*

class CodeforcesContestWatcher(val handle: String, val contestID: Int, val scope: CoroutineScope): CodeforcesContestWatchListener(){

    fun address(contestID: Int, handle: String, participationType: ParticipationType): String {
        return "https://codeforces.com/api/contest.standings?contestId=$contestID&handles=$handle&showUnofficial=" + if(participationType == ParticipationType.OFFICIAL) "false" else "true"
    }

    enum class ParticipationType {
        NOTPARTICIPATED, OFFICIAL, UNOFFICIAL
    }

    enum class ContestType {
        CF, ICPC, IOI, UNDEFINED
    }

    private var job: Job? = null

    fun start(){
        job = scope.launch {
            val contestName = ChangingValue("")
            val contestType = ChangingValue(ContestType.UNDEFINED)
            val phaseCodeforces = ChangingValue(CodeforcesContestPhase.UNKNOWN)
            val rank = ChangingValue(-1)
            val pointsTotal = ChangingValue(0.0)
            val durationSeconds = ChangingValue(-1)
            val startTimeSeconds = ChangingValue(-1)
            var problemNames: ArrayList<String>? = null
            val participationType = ChangingValue(ParticipationType.NOTPARTICIPATED)
            val sysTestPercentage = ChangingValue(-1)
            var problemsPoints: List<ChangingValue<Pair<Double, String>>> = emptyList()

            while (true) {

                var timeSecondsFromStart: Int? = null
                try {
                    withContext(Dispatchers.IO) {
                        with(JsonReaderFromURL(address(contestID, handle, participationType.value)) ?: return@withContext) {
                            beginObject()
                            if (nextString("status") == "FAILED"){
                                val reason = nextString("comment")
                                if(reason == "contestId: Contest with id $contestID has not started")
                                    phaseCodeforces.value = CodeforcesContestPhase.BEFORE
                                return@withContext
                            }
                            nextName()
                            readObject {
                                while (hasNext()) when (nextName()) {
                                    "contest" -> readObject {
                                        while (hasNext()) when (nextName()) {
                                            "phase" -> phaseCodeforces.value = CodeforcesContestPhase.valueOf(nextString())
                                            "relativeTimeSeconds" -> timeSecondsFromStart = nextInt()
                                            "durationSeconds" -> durationSeconds.value = nextInt()
                                            "startTimeSeconds" -> startTimeSeconds.value = nextInt()
                                            "name" -> contestName.value = nextString()
                                            "type" -> contestType.value = ContestType.valueOf(nextString())
                                            else -> skipValue()
                                        }
                                    }
                                    "problems" -> {
                                        if (problemNames == null) {
                                            val tmp = ArrayList<String>()
                                            readArray { tmp.add(readObjectFields("index")[0] as String) }
                                            problemNames = tmp
                                            onSetProblemNames(tmp.toTypedArray())
                                        } else skipValue()
                                    }
                                    "rows" -> readArrayOfObjects {
                                        lateinit var tp : String
                                        val problemResults = arrayListOf<Pair<Double,String>>()
                                        while (hasNext()) when (nextName()) {
                                            "party" -> {
                                                tp = readObjectFields("participantType")[0] as String
                                                when(tp){
                                                    "CONTESTANT" -> participationType.value = ParticipationType.OFFICIAL
                                                    "OUT_OF_COMPETITION" -> participationType.value = ParticipationType.UNOFFICIAL
                                                }
                                            }
                                            "rank" -> if(tp=="PRACTICE") skipValue() else rank.value = nextInt()
                                            "points" -> if(tp=="PRACTICE") skipValue() else pointsTotal.value = nextDouble()
                                            "problemResults" -> if(tp=="PRACTICE") skipValue() else readArray {
                                                val arr = readObjectFields("points", "type")
                                                val pts = arr[0] as Double
                                                val status = arr[1] as String
                                                problemResults.add(Pair(pts, status))
                                            }
                                            else -> skipValue()
                                        }
                                        if(tp != "PRACTICE") {
                                            if (problemResults.size != problemsPoints.size) problemsPoints = problemResults.map { ChangingValue(it, true) }
                                            else problemResults.forEachIndexed { index, pair -> problemsPoints[index].value = pair }
                                        }
                                    }
                                    else -> skipValue()
                                }
                            }
                            this.close()
                        }
                    }
                }catch (e: JsonEncodingException){

                }catch (e: JsonDataException){

                }


                if(contestName.isChanged() || contestType.isChanged()) onSetContestNameAndType(contestName.value, contestType.value)
                if(phaseCodeforces.isChanged()) onSetContestPhase(phaseCodeforces.value)

                if(phaseCodeforces.value == CodeforcesContestPhase.CODING && (durationSeconds.isChanged() || startTimeSeconds.isChanged())) timeSecondsFromStart?.let{
                    val remainingTime = durationSeconds.value - it
                    onSetRemainingTime(remainingTime)
                }

                if(phaseCodeforces.value == CodeforcesContestPhase.SYSTEM_TEST){
                    //get progress of testing (0% ... 100%)
                    CodeforcesAPI.getPageSource("contest/$contestID", "en")?.let { page ->
                        var i = page.indexOf("<span class=\"contest-state-regular\">")
                        if (i != -1) {
                            i = page.indexOf(">", i + 1)
                            val progress = page.substring(i + 1, page.indexOf("</", i + 1))
                            if (progress.endsWith('%')) {
                                sysTestPercentage.value = progress.substring(0, progress.length - 1).toInt()
                                if(sysTestPercentage.isChanged()) onSetSysTestProgress(sysTestPercentage.value)
                            }
                        }
                    }
                }


                if(participationType.isChanged()){
                    onSetParticipationType(participationType.value)
                    if(participationType.value == ParticipationType.OFFICIAL){
                        pointsTotal.value = 0.0
                        rank.value = -1
                        problemsPoints = emptyList()
                        continue
                    }
                }

                if(rank.value != -1){
                    if(rank.isChanged()) onSetContestantRank(rank.value)
                    if(pointsTotal.isChanged()) onSetContestantPoints(pointsTotal.value)

                    problemsPoints.forEachIndexed { index, changingValue ->
                        if(changingValue.isChanged()){
                            val (pts, status) = changingValue.value
                            val problemName = problemNames!![index]
                            onSetProblemStatus(problemName, status, pts)
                        }
                        if(phaseCodeforces.value == CodeforcesContestPhase.SYSTEM_TEST){
                            //TODO("watch problem passed/failed systest")
                        }
                    }
                }


                //------------------------
                commit()
                when(phaseCodeforces.value){
                    CodeforcesContestPhase.CODING -> delay(3_000)
                    CodeforcesContestPhase.SYSTEM_TEST -> delay(5_000)
                    CodeforcesContestPhase.PENDING_SYSTEM_TEST -> delay(15_000)
                    CodeforcesContestPhase.FINISHED -> {
                        //TODO("watch rating changes")
                        return@launch
                    }
                    else -> delay(30_000)
                }
            }
        }
    }

    fun stop(){
        job?.cancel()
    }




    private val listeners = mutableListOf<CodeforcesContestWatchListener>()

    fun addCodeforcesContestWatchListener(listener: CodeforcesContestWatchListener) = listeners.add(listener)

    override fun onSetContestNameAndType(contestName: String, contestType: ContestType) {
        listeners.forEach { l -> l.onSetContestNameAndType(contestName, contestType) }
    }

    override suspend fun onSetProblemNames(problemNames: Array<String>) {
        listeners.forEach { l -> l.onSetProblemNames(problemNames) }
    }

    override fun onSetContestPhase(phaseCodeforces: CodeforcesContestPhase) {
        listeners.forEach { l -> l.onSetContestPhase(phaseCodeforces) }
    }

    override fun onSetRemainingTime(timeSeconds: Int) {
        listeners.forEach { l -> l.onSetRemainingTime(timeSeconds) }
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

    override fun onSetProblemStatus(problem: String, status: String, points: Double) {
        listeners.forEach { l -> l.onSetProblemStatus(problem, status, points) }
    }

    override fun onSetParticipationType(type: ParticipationType) {
        listeners.forEach { l -> l.onSetParticipationType(type) }
    }

    override fun commit() {
        listeners.forEach { l -> l.commit() }
    }
}


enum class CodeforcesContestPhase{
    UNKNOWN,
    BEFORE,
    CODING,
    PENDING_SYSTEM_TEST,
    SYSTEM_TEST,
    FINISHED
}

abstract class CodeforcesContestWatchListener{
    abstract fun onSetContestNameAndType(contestName: String, contestType: CodeforcesContestWatcher.ContestType)
    abstract suspend fun onSetProblemNames(problemNames: Array<String>)
    abstract fun onSetContestPhase(phaseCodeforces: CodeforcesContestPhase)
    abstract fun onSetRemainingTime(timeSeconds: Int)
    abstract fun onSetSysTestProgress(percents: Int)
    abstract fun onSetContestantRank(rank: Int)
    abstract fun onSetContestantPoints(points: Double)
    abstract fun onSetProblemStatus(problem: String, status: String, points: Double)
    abstract fun onSetParticipationType(type: CodeforcesContestWatcher.ParticipationType)
    abstract fun commit()
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