package com.example.test3

import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import kotlinx.coroutines.*

class CodeforcesContestWatcher(val handle: String, val contestID: Int, val scope: CoroutineScope): CodeforcesContestWatchListener(){

    private val address = "https://codeforces.com/api/contest.standings?contestId=$contestID&showUnofficial=false&handles=$handle"

    private var job: Job? = null

    fun start(){
        job = scope.launch {
            var rank: Int? = null
            var pointsTotal: Int? = null
            var timeSecondsFromStart: Int? = null
            var durationSeconds: Int? = null
            var tasksPrevious: ArrayList<Pair<Int, String>>? = null
            var problemNames: ArrayList<String>? = null
            var contestName = ""

            while (true) {
                var phaseCodeforces = CodeforcesContestPhase.UNKNOWN
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
                                            "phase" -> phaseCodeforces = CodeforcesContestPhase.valueOf(nextString())
                                            "relativeTimeSeconds" -> timeSecondsFromStart = nextInt()
                                            "durationSeconds" -> durationSeconds = nextInt()
                                            "name" -> contestName = nextString()
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
                                            "rank" -> rank = nextInt()
                                            "points" -> pointsTotal = nextInt()
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


                onSetContestName(contestName)
                if(phaseCodeforces != CodeforcesContestPhase.UNKNOWN) onSetContestPhase(phaseCodeforces)

                if(phaseCodeforces == CodeforcesContestPhase.SYSTEM_TEST){
                    //get progress of testing (0% ... 100%)
                    readURLData("https://codeforces.com/contest/$contestID")?.let { page ->
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

                if(phaseCodeforces == CodeforcesContestPhase.CODING) timeSecondsFromStart?.let{
                    val remainingTime = durationSeconds!! - it
                    val SS = remainingTime % 60
                    val MM = remainingTime / 60 % 60
                    val HH = remainingTime / 60 / 60
                    val time_str = String.format("%02d:%02d:%02d", HH, MM, SS)
                    onSetRemainingTime(time_str)
                }


                rank?.let{
                    onSetContestantRank(it)
                    onSetContestantPoints(pointsTotal!!)

                    tasks.forEachIndexed { index, (pts, status) ->
                        val problemName = problemNames!![index]
                        onSetProblemStatus(problemName, status, pts)
                        if(phaseCodeforces == CodeforcesContestPhase.SYSTEM_TEST){
                            tasksPrevious?.let {
                                if(it[index].second!=status){
                                    //onSetProblemStatus(problemName, status)

                                }
                            }
                        }
                    }
                    tasksPrevious = tasks
                }


                if(phaseCodeforces == CodeforcesContestPhase.FINISHED){
                    //TODO check rating update
                    return@launch
                }


                //------------------------
                when(phaseCodeforces){
                    CodeforcesContestPhase.CODING -> delay(2_000)
                    CodeforcesContestPhase.SYSTEM_TEST -> delay(3_000)
                    CodeforcesContestPhase.FINISHED -> delay(30_000)
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

    override fun onSetRemainingTime(time: String) {
        listeners.forEach { l -> l.onSetRemainingTime(time) }
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
    abstract fun onSetRemainingTime(time: String)
    abstract fun onSetSysTestProgress(percents: Int)
    abstract fun onSetContestantRank(rank: Int)
    abstract fun onSetContestantPoints(points: Int)
    abstract fun onSetProblemStatus(problem: String, status: String, points: Int)
}