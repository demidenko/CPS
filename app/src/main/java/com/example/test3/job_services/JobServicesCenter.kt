package com.example.test3.job_services

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import androidx.preference.PreferenceManager
import com.example.test3.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

object JobServiceIDs {
    private var id = 0
    val news_parsers = ++id
    val accounts_parsers = ++id
    val codeforces_lost_recent_news = ++id
    val project_euler_recent_problems = ++id
}

object JobServicesCenter {
    private fun makeSchedule(
        context: Context,
        id: Int,
        c: Class<*>?,
        millis: Long,
        network_type: Int
    ) {
        val builder = JobInfo.Builder(id, ComponentName(context, c!!)).apply {
            setPeriodic(millis)
            setRequiredNetworkType(network_type)
        }
        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        jobScheduler.schedule(builder.build())
    }

    fun getRunningJobServices(context: Context): List<JobInfo> {
        val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        return scheduler.allPendingJobs
    }

    fun stopJobService(context: Context, jobID: Int) {
        val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        scheduler.cancel(jobID)
    }

    fun startJobServices(context: Context){
        val toStart = mutableMapOf<Int, (Context)->Unit >(
            JobServiceIDs.news_parsers to ::startNewsJobService,
            JobServiceIDs.accounts_parsers to ::startAccountsJobService,
            JobServiceIDs.project_euler_recent_problems to ::startProjectEulerRecentProblemsJobService
        )
        if(PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.news_codeforces_lost_enabled), false)){
            toStart[JobServiceIDs.codeforces_lost_recent_news] = ::startCodeforcesNewsLostRecentJobService
        }
        getRunningJobServices(context).forEach {
            if(toStart.containsKey(it.id)) toStart.remove(it.id)
            else stopJobService(context, it.id)
        }
        toStart.values.shuffled().forEach { it(context) }
    }

    private fun startNewsJobService(context: Context){
        makeSchedule(
            context,
            JobServiceIDs.news_parsers,
            NewsJobService::class.java,
            TimeUnit.HOURS.toMillis(16),
            JobInfo.NETWORK_TYPE_ANY
        )
    }

    fun startCodeforcesNewsLostRecentJobService(context: Context){
        makeSchedule(
            context,
            JobServiceIDs.codeforces_lost_recent_news,
            CodeforcesNewsLostRecentJobService::class.java,
            TimeUnit.HOURS.toMillis(1),
            JobInfo.NETWORK_TYPE_ANY
        )
    }

    private fun startProjectEulerRecentProblemsJobService(context: Context){
        makeSchedule(
            context,
            JobServiceIDs.project_euler_recent_problems,
            ProjectEulerRecentProblemsJobService::class.java,
            TimeUnit.HOURS.toMillis(1),
            JobInfo.NETWORK_TYPE_ANY
        )
    }

    private fun startAccountsJobService(context: Context){
        makeSchedule(
            context,
            JobServiceIDs.accounts_parsers,
            AccountsJobService::class.java,
            TimeUnit.MINUTES.toMillis(15),
            JobInfo.NETWORK_TYPE_ANY
        )
    }

}

abstract class CoroutineJobService : JobService(), CoroutineScope{

    override val coroutineContext: CoroutineContext = Job() + Dispatchers.Main

    protected abstract suspend fun doJob()

    override fun onStopJob(params: JobParameters?): Boolean {
        return false
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        launch {
            doJob()
            jobFinished(params, false)
        }
        return true
    }
}