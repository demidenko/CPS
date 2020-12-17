package com.example.test3.job_services

import com.example.test3.account_manager.CodeforcesAccountManager
import com.example.test3.account_manager.STATUS
import com.example.test3.contest_watch.CodeforcesContestWatchService
import com.example.test3.utils.CodeforcesAPI
import com.example.test3.utils.CodeforcesAPIStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class CodeforcesContestWatchStarterJobService: CoroutineJobService() {

    private val accountManager by lazy { CodeforcesAccountManager(this) }
    override suspend fun makeJobs(): ArrayList<Job> {
        if(accountManager.getSettings().getContestWatchEnabled()){
            return arrayListOf(launch { checkSubmissions() })
        }
        return arrayListOf()
    }

    private suspend fun checkSubmissions() {
        val info = accountManager.getSavedInfo() as CodeforcesAccountManager.CodeforcesUserInfo
        if(info.status != STATUS.OK) return

        val currentTimeSeconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())
        fun isTooLate(timeSeconds: Long): Boolean {
            return TimeUnit.SECONDS.toHours(currentTimeSeconds - timeSeconds) >= 24
        }

        val settings = accountManager.getSettings()
        val lastKnownID: Long = settings.getContestWatchLastSubmissionID()

        var firstID: Long? = null
        var step = 1 //ask 1, than 10 util
        var from = 1
        while (true) {
            val response = CodeforcesAPI.getUserSubmissions(info.handle, step, from) ?: return
            if(response.status != CodeforcesAPIStatus.OK) return

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
                    firstID?.let { id ->
                        settings.setContestWatchLastSubmissionID(id)
                    }
                    if(result != -1){
                        CodeforcesContestWatchService.startService(this, info.handle, result)
                    }
                    return
                }
            }

        }
    }

}