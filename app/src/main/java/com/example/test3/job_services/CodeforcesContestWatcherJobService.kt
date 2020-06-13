package com.example.test3.job_services

import android.app.job.JobParameters
import android.app.job.JobService
import com.example.test3.JsonReaderFromURL
import com.example.test3.account_manager.CodeforcesAccountManager
import com.example.test3.account_manager.STATUS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class CodeforcesContestWatcherJobService: JobService(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Job() + Dispatchers.Main

    override fun onStopJob(params: JobParameters?): Boolean {
        return false
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        launch {
            job()
            jobFinished(params, false)
        }
        return true
    }

    suspend fun job() {
        val info = CodeforcesAccountManager(this).savedInfo
        if(info.status != STATUS.OK) return

        with(JsonReaderFromURL("https://codeforces.com/api/user.status?handle=${info.userID}&count=50") ?: return){

        }

    }

}