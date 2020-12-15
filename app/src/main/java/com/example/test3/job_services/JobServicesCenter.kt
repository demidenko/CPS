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

    private fun getJobScheduler(context: Context) = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler

    private fun makeSchedule(
        context: Context,
        id: Int,
        c: Class<*>,
        millis: Long,
        flex: Long = millis
    ) {
        val builder = JobInfo.Builder(id, ComponentName(context, c)).apply {
            setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            setPersisted(true)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N)
                setPeriodic(millis, flex)
            else setPeriodic(millis)
        }
        getJobScheduler(context).schedule(builder.build())
    }

    fun getRunningJobServices(context: Context): List<JobInfo> = getJobScheduler(context).allPendingJobs

    fun stopJobService(context: Context, jobID: Int) = getJobScheduler(context).cancel(jobID)

    fun startJobServices(mainActivity: MainActivity){
        val toStart = mutableMapOf<Int, (Context)->Unit >(
            JobServiceIDs.news_parsers to ::startNewsJobService,
            JobServiceIDs.accounts_parsers to ::startAccountsJobService
        )
        with(PreferenceManager.getDefaultSharedPreferences(mainActivity)){
            if(getBoolean(mainActivity.getString(R.string.news_codeforces_lost_enabled), false)) toStart[JobServiceIDs.codeforces_news_lost_recent] = ::startCodeforcesNewsLostRecentJobService
            if(getBoolean(mainActivity.getString(R.string.news_codeforces_follow_enabled), false)) toStart[JobServiceIDs.codeforces_news_follow] = ::startCodeforcesNewsFollowJobService
            if(getBoolean(mainActivity.getString(R.string.news_project_euler_problems), false)) toStart[JobServiceIDs.project_euler_recent_problems] = ::startProjectEulerRecentProblemsJobService
        }
        getRunningJobServices(mainActivity).forEach {
            if(toStart.containsKey(it.id)) toStart.remove(it.id)
            else stopJobService(mainActivity, it.id)
        }
        mainActivity.lifecycleScope.launchWhenStarted {
            val progressInfo = BottomProgressInfo(toStart.size, "start services", mainActivity)
            toStart.values.shuffled().forEach { start ->
                delay(500)
                start(mainActivity)
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
            TimeUnit.HOURS.toMillis(6)
        )
    }

    fun startCodeforcesNewsLostRecentJobService(context: Context){
        makeSchedule(
            context,
            JobServiceIDs.codeforces_news_lost_recent,
            CodeforcesNewsLostRecentJobService::class.java,
            TimeUnit.HOURS.toMillis(1)
        )
    }

    fun startCodeforcesNewsFollowJobService(context: Context){
        makeSchedule(
            context,
            JobServiceIDs.codeforces_news_follow,
            CodeforcesNewsFollowJobService::class.java,
            TimeUnit.HOURS.toMillis(6),
            TimeUnit.HOURS.toMillis(3)
        )
    }

    fun startProjectEulerRecentProblemsJobService(context: Context){
        makeSchedule(
            context,
            JobServiceIDs.project_euler_recent_problems,
            ProjectEulerRecentProblemsJobService::class.java,
            TimeUnit.HOURS.toMillis(1)
        )
    }

    fun startAccountsJobService(context: Context){
        makeSchedule(
            context,
            JobServiceIDs.accounts_parsers,
            AccountsJobService::class.java,
            TimeUnit.MINUTES.toMillis(15)
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