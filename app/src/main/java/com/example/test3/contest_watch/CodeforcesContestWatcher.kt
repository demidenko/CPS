package com.example.test3.contest_watch

import com.example.test3.*
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import kotlinx.coroutines.*

class CodeforcesContestWatcher(val handle: String, val contestID: Int, val scope: CoroutineScope): CodeforcesContestWatchListener(){

    private val address = "https://codeforces.com/api/contest.standings?contestId=$contestID&showUnofficial=false&handles=$handle"

    private var job: Job? = null

    fun start(){
        job = scope.launch {
            val contestName = ChangingValue("")
            val rank = ChangingValue(-1)
            val pointsTotal = ChangingValue(0)
            var timeSecondsFromStart: Int? = null
            var durationSeconds: Int? = null
            var tasksPrevious: ArrayList<Pair<Int, String>>? = null
            var problemNames: ArrayList<String>? = null

            while (true) {
                val phaseCodeforces = ChangingValue(CodeforcesContestPhase.UNKNOWN)
                val tasks = arrayListOf<Pair<Int,String>>()
                try {
                    withContext(Dispatchers.IO) {
                        with(JsonReaderFromURL(address) ?: return@withContext) {
                            beginObject()
                            if (nextString("status") == "FAILED") return@withContext
                            nextName()
                            readObject {
                                while (hasNext()) when (nextName()) {
                                    "contest" -> readObject {
                                        while (hasNext()) when (nextName()) {
                                            "phase" -> phaseCodeforces.value = CodeforcesContestPhase.valueOf(nextString())
                                            "relativeTimeSeconds" -> timeSecondsFromStart = nextInt()
                                            "durationSeconds" -> durationSeconds = nextInt()
                                            "name" -> contestName.value = nextString()
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
                                        while (hasNext()) when (nextName()) {
                                            "rank" -> rank.value = nextInt()
                                            "points" -> pointsTotal.value = nextInt()
                                            "problemResults" -> readArray {
                                                val arr = readObjectFields("points", "type")
                                                val pts = (arr[0] as Double).toInt()
                                                val status = arr[1] as String
                                                tasks.add(Pair(pts, status))
                                            }
                                            else -> skipValue()
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


                if(contestName.isChanged()) onSetContestName(contestName.value)
                if(phaseCodeforces.isChanged()) onSetContestPhase(phaseCodeforces.value)

                if(phaseCodeforces.value == CodeforcesContestPhase.SYSTEM_TEST){
                    //get progress of testing (0% ... 100%)
                    readURLData("https://codeforces.com/contest/$contestID")
                        ?.let { page ->
                        var i = page.indexOf("<span class=\"contest-state-regular\">")
                        if (i != -1) {
                            i = page.indexOf(">", i + 1)
                            val progress = page.substring(i + 1, page.indexOf("</", i + 1))
                            if (progress.isNotEmpty() && progress.last() == '%') {
                                onSetSysTestProgress(progress.substring(0, progress.length - 1).toInt())
                            }
                        }
                    }
                }

                if(phaseCodeforces.value == CodeforcesContestPhase.CODING) timeSecondsFromStart?.let{
                    val remainingTime = durationSeconds!! - it
                    onSetRemainingTime(remainingTime)
                }


                if(rank.value != -1){
                    if(rank.isChanged()) onSetContestantRank(rank.value)
                    if(pointsTotal.isChanged()) onSetContestantPoints(pointsTotal.value)

                    tasks.forEachIndexed { index, (pts, status) ->
                        val problemName = problemNames!![index]
                        onSetProblemStatus(problemName, status, pts)
                        if(phaseCodeforces.value == CodeforcesContestPhase.SYSTEM_TEST){
                            tasksPrevious?.let { prev ->
                                if(prev[index].second!=status){
                                    //onSetProblemStatus(problemName, status)

                                }
                            }
                        }
                    }
                    tasksPrevious = tasks
                }


                //------------------------
                commit()
                when(phaseCodeforces.value){
                    CodeforcesContestPhase.CODING -> delay(2_000)
                    CodeforcesContestPhase.SYSTEM_TEST -> delay(3_000)
                    CodeforcesContestPhase.FINISHED -> return@launch
                    else -> delay(60_000)
                }
            }
        }
    }

    fun stop(){
        job?.cancel()
    }




    private val listeners = mutableListOf<CodeforcesContestWatchListener>()

    fun addCodeforcesContestWatchListener(listener: CodeforcesContestWatchListener) = listeners.add(listener)

    override fun onSetContestName(contestName: String) {
        listeners.forEach { l -> l.onSetContestName(contestName) }
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

    override fun onSetContestantPoints(points: Int) {
        listeners.forEach { l -> l.onSetContestantPoints(points) }
    }

    override fun onSetProblemStatus(problem: String, status: String, points: Int) {
        listeners.forEach { l -> l.onSetProblemStatus(problem, status, points) }
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
    abstract fun onSetContestName(contestName: String)
    abstract suspend fun onSetProblemNames(problemNames: Array<String>)
    abstract fun onSetContestPhase(phaseCodeforces: CodeforcesContestPhase)
    abstract fun onSetRemainingTime(timeSeconds: Int)
    abstract fun onSetSysTestProgress(percents: Int)
    abstract fun onSetContestantRank(rank: Int)
    abstract fun onSetContestantPoints(points: Int)
    abstract fun onSetProblemStatus(problem: String, status: String, points: Int)
    abstract fun commit()
}


class ChangingValue<T>(private var x: T){

    private var changingFlag: Boolean = false

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