package com.example.test3.job_services

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.example.test3.BottomProgressInfo
import com.example.test3.MainActivity
import com.example.test3.R
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

object JobServiceIDs {
    private var id = 0
    val news_parsers = ++id
    val accounts_parsers = ++id
    val codeforces_news_lost_recent = ++id
    val codeforces_news_follow = ++id
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

    fun startJobServices(activity: MainActivity){
        val toStart = mutableMapOf<Int, (Context)->Unit >(
            JobServiceIDs.news_parsers to ::startNewsJobService,
            JobServiceIDs.accounts_parsers to ::startAccountsJobService,
            JobServiceIDs.project_euler_recent_problems to ::startProjectEulerRecentProblemsJobService
        )
        with(PreferenceManager.getDefaultSharedPreferences(activity)){
            if(getBoolean(activity.getString(R.string.news_codeforces_lost_enabled), false)) toStart[JobServiceIDs.codeforces_news_lost_recent] = ::startCodeforcesNewsLostRecentJobService
            if(getBoolean(activity.getString(R.string.news_codeforces_follow_enabled), false)) toStart[JobServiceIDs.codeforces_news_follow] = ::startCodeforcesNewsFollowJobService
        }
        getRunningJobServices(activity).forEach {
            if(toStart.containsKey(it.id)) toStart.remove(it.id)
            else stopJobService(activity, it.id)
        }
        activity.lifecycleScope.launchWhenStarted {
            val progressInfo = BottomProgressInfo(toStart.size, "start services", activity)
            toStart.values.shuffled().forEach { start ->
                delay(500)
                start(activity)
                progressInfo.increment()
            }
            progressInfo.finish()
        }
    }

    fun startNewsJobService(context: Context){
        makeSchedule(
            context,
            JobServiceIDs.news_parsers,
            NewsJobService::class.java,
            TimeUnit.HOURS.toMillis(6),
            JobInfo.NETWORK_TYPE_ANY
        )
    }

    fun startCodeforcesNewsLostRecentJobService(context: Context){
        makeSchedule(
            context,
            JobServiceIDs.codeforces_news_lost_recent,
            CodeforcesNewsLostRecentJobService::class.java,
            TimeUnit.HOURS.toMillis(1),
            JobInfo.NETWORK_TYPE_ANY
        )
    }

    fun startCodeforcesNewsFollowJobService(context: Context){
        makeSchedule(
            context,
            JobServiceIDs.codeforces_news_follow,
            CodeforcesNewsFollowJobService::class.java,
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
            TimeUnit.HOURS.toMillis(1),
            JobInfo.NETWORK_TYPE_ANY
        )
    }

}

abstract class CoroutineJobService : JobService(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Job() + Dispatchers.Main

    protected abstract suspend fun makeJobs(): ArrayList<Job>

    override fun onStopJob(params: JobParameters?): Boolean {
        coroutineContext.cancelChildren()
        return false
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        launch {
            makeJobs().joinAll()
            jobFinished(params, false)
        }
        return true
    }
}