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

    override suspend fun doWork(): Result {

        val codeforcesAccountManager = CodeforcesAccountManager(context)

        if(!codeforcesAccountManager.getSettings().contestWatchEnabled()){
            WorkersCenter.stopWorker(context, WorkersNames.codeforces_contest_watch_launcher)
            return Result.success()
        }

        val info = codeforcesAccountManager.getSavedInfo()
        if(info.status != STATUS.OK) return Result.success()

        val currentTimeSeconds = getCurrentTimeSeconds()
        fun isTooLate(timeSeconds: Long): Boolean {
            return TimeUnit.SECONDS.toHours(currentTimeSeconds - timeSeconds) >= 24
        }

        val lastKnownID: Long?
        val canceled: List<Pair<Int,Long>>
        with(codeforcesAccountManager.getSettings()){
            lastKnownID = contestWatchLastSubmissionID()
            canceled = contestWatchCanceled().toMutableList().apply {
                if(removeAll { (id, timeSeconds) -> isTooLate(timeSeconds) }){
                    contestWatchCanceled(this)
                }
            }
        }

        var firstID: Long? = null
        var step = 1 //ask 1, than 10 util
        var from = 1
        while (true) {
            val response = CodeforcesAPI.getUserSubmissions(info.handle, step, from) ?: return Result.retry()
            if(response.status != CodeforcesAPIStatus.OK) return Result.retry()

            val resultId = response.result!!.let { submissions ->
                for(submission in submissions){
                    if(firstID == null) firstID = submission.id
                    if(lastKnownID != null && submission.id <= lastKnownID) return@let null
                    if(isTooLate(submission.creationTimeSeconds)) return@let null
                    if(submission.author.participantType.participatedInContest()) return@let submission.contestId
                }
                CONTINUE
            }

            if(resultId == CONTINUE) {
                from+=step
                step = 10
                continue
            }

            val settings = codeforcesAccountManager.getSettings().apply {
                contestWatchLastSubmissionID(firstID!!)
            }

            (resultId ?: settings.contestWatchStartedContestID())
                ?.let { contestID ->
                    if(canceled.none { it.first == contestID }) {
                        settings.contestWatchStartedContestID(contestID)
                        CodeforcesContestWatchWorker.startWorker(context, info.handle, contestID)
                    }
                }

            return Result.success()
        }
    }

    companion object {
        fun onStopWatcher(context: Context, contestID: Int) = runBlocking {
            with(CodeforcesAccountManager(context).getSettings()){
                contestWatchStartedContestID(null)
                val canceled = contestWatchCanceled().toMutableList()
                canceled.add(contestID to getCurrentTimeSeconds())
                contestWatchCanceled(canceled)
            }
        }

        private const val CONTINUE = 0
    }

}