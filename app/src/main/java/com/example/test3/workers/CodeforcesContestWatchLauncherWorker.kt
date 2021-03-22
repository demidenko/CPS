package com.example.test3.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.test3.account_manager.CodeforcesAccountManager
import com.example.test3.account_manager.STATUS
import com.example.test3.contest_watch.CodeforcesContestWatchWorker
import com.example.test3.utils.CodeforcesAPI
import com.example.test3.utils.CodeforcesAPIStatus
import com.example.test3.utils.getCurrentTimeSeconds
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit

class CodeforcesContestWatchLauncherWorker(private val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val codeforcesAccountManager by lazy { CodeforcesAccountManager(context) }

    override suspend fun doWork(): Result {

        if(!codeforcesAccountManager.getSettings().getContestWatchEnabled()){
            WorkersCenter.stopWorker(context, WorkersNames.codeforces_contest_watch_launcher)
            return Result.success()
        }

        val info = codeforcesAccountManager.getSavedInfo() as CodeforcesAccountManager.CodeforcesUserInfo
        if(info.status != STATUS.OK) return Result.success()

        val currentTimeSeconds = getCurrentTimeSeconds()
        fun isTooLate(timeSeconds: Long): Boolean {
            return TimeUnit.SECONDS.toHours(currentTimeSeconds - timeSeconds) >= 24
        }

        val lastKnownID: Long
        val canceled: List<Pair<Int,Long>>
        with(codeforcesAccountManager.getSettings()){
            lastKnownID = getContestWatchLastSubmissionID()
            canceled = getContestWatchCanceled().filter { (id, timeSeconds) -> !isTooLate(timeSeconds) }
            setContestWatchCanceled(canceled)
        }

        var firstID: Long? = null
        var step = 1 //ask 1, than 10 util
        var from = 1
        while (true) {
            val response = CodeforcesAPI.getUserSubmissions(info.handle, step, from) ?: return Result.retry()
            if(response.status != CodeforcesAPIStatus.OK) return Result.retry()

            val result = response.result!!.let { submissions ->
                for(submission in submissions){
                    if(firstID == null) firstID = submission.id
                    if(submission.id <= lastKnownID) return@let -1
                    if(isTooLate(submission.creationTimeSeconds)) return@let -1
                    if(submission.author.participantType.participatedInContest()) return@let submission.contestId
                }
                0
            }

            /*
                id>0    -> run service
                0       -> continue
                -1      -> break
             */

            when(result) {
                0 -> {
                    from+=step
                    step = 10
                    continue
                }
                else -> {
                    val settings = codeforcesAccountManager.getSettings()
                    firstID?.let { id ->
                        settings.setContestWatchLastSubmissionID(id)
                    }
                    val contestID =
                        if(result != -1) result
                        else settings.getContestWatchStartedContestID()
                    if(contestID != -1){
                        if(canceled.none { it.first == contestID }) {
                            settings.setContestWatchStartedContestID(contestID)
                            CodeforcesContestWatchWorker.startWorker(context, info.handle, contestID)
                        }
                    }
                    return Result.success()
                }
            }

        }
    }

    companion object {
        fun stopWatcher(context: Context, contestID: Int) = runBlocking {
            with(CodeforcesAccountManager(context).getSettings()){
                setContestWatchStartedContestID(-1)
                val canceled = getContestWatchCanceled().toMutableList()
                canceled.add(contestID to getCurrentTimeSeconds())
                setContestWatchCanceled(canceled)
            }
        }
    }

}