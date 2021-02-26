package com.example.test3.job_services

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.example.test3.BottomProgressInfo
import com.example.test3.MainActivity
import com.example.test3.SettingsDelegate
import com.example.test3.SettingsDev
import com.example.test3.account_manager.CodeforcesAccountManager
import com.example.test3.news.SettingsNewsFragment
import com.example.test3.utils.SettingsDataStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

object JobServiceIDs {
    private var id = 0
    val news_parsers = ++id
    val accounts_parsers = ++id
    val codeforces_news_lost_recent = ++id
    val codeforces_news_follow = ++id
    val codeforces_contest_watch_starter = ++id
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

    suspend fun startJobServices(mainActivity: MainActivity){
        val toStart = mutableMapOf<Int, (Context)->Unit >(
            JobServiceIDs.news_parsers to ::startNewsJobService,
            JobServiceIDs.accounts_parsers to ::startAccountsJobService
        )
        with(SettingsNewsFragment.getSettings(mainActivity)){
            if(getLostEnabled()) toStart[JobServiceIDs.codeforces_news_lost_recent] = ::startCodeforcesNewsLostRecentJobService
            if(getFollowEnabled()) toStart[JobServiceIDs.codeforces_news_follow] = ::startCodeforcesNewsFollowJobService
            if(getNewsFeedEnabled(SettingsNewsFragment.NewsFeed.PROJECT_EULER_RECENT)) toStart[JobServiceIDs.project_euler_recent_problems] = ::startProjectEulerRecentProblemsJobService
        }
        with(CodeforcesAccountManager(mainActivity).getSettings()){
            if(getContestWatchEnabled()) toStart[JobServiceIDs.codeforces_contest_watch_starter] = ::startCodeforcesContestWatchStarterJobService
        }

        getRunningJobServices(mainActivity).forEach {
            if(toStart.containsKey(it.id)) toStart.remove(it.id)
            else stopJobService(mainActivity, it.id)
        }

        val progressInfo = BottomProgressInfo("start services", mainActivity).takeIf { SettingsDev(mainActivity).getDevEnabled() } ?.start(toStart.size)
        toStart.values.shuffled().forEach { start ->
            delay(500)
            start(mainActivity)
            progressInfo?.increment()
        }
        progressInfo?.finish()
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

    fun startCodeforcesContestWatchStarterJobService(context: Context){
        makeSchedule(
            context,
            JobServiceIDs.codeforces_contest_watch_starter,
            CodeforcesContestWatchStarterJobService::class.java,
            TimeUnit.MINUTES.toMillis(45)
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

abstract class CoroutineJobService() : JobService(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Job() + Dispatchers.Main

    protected abstract suspend fun makeJobs(): List<Job>

    override fun onStopJob(params: JobParameters?): Boolean {
        coroutineContext.cancelChildren()
        return false
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        launch {
            settingsJobServices.setStartTimeMillis(params!!.jobId, System.currentTimeMillis())
            makeJobs().joinAll()
            jobFinished(params, false)
        }
        return true
    }
}

val Context.settingsJobServices by SettingsDelegate { SettingsJobServices(it) }

class SettingsJobServices(context: Context): SettingsDataStore(context, "data_job_services") {

    private fun makeStartTimeKey(jobId: Int) = longPreferencesKey("start_time_$jobId")

    suspend fun setStartTimeMillis(jobId: Int, timeMillis: Long){
        dataStore.edit { it[makeStartTimeKey(jobId)] = timeMillis }
    }

    suspend fun getStartTimeMillis(jobId: Int) = dataStore.data.first()[makeStartTimeKey(jobId)] ?: 0L
}