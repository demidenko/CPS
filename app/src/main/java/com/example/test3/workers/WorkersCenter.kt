package com.example.test3.workers

import android.content.Context
import androidx.work.*
import com.example.test3.MainActivity
import com.example.test3.account_manager.CodeforcesAccountManager
import java.util.concurrent.TimeUnit

object WorkersNames {
    const val accounts_parsers = "accounts"
    const val news_parsers = "news_feeds"
    const val codeforces_news_lost_recent = "cf_lost"
    const val codeforces_news_follow = "cf_follow"
    const val codeforces_contest_watch_launcher = "cf_contest_watch_launcher"
    const val project_euler_recent_problems = "pe_recent"
}

object WorkersCenter {

    private fun getWorkManager(context: Context) = WorkManager.getInstance(context)

    const val commonTag = "cps"
    fun getWorksLiveData(context: Context) = getWorkManager(context).getWorkInfosByTagLiveData(commonTag)

    private inline fun<reified T: CoroutineWorker> makeAndEnqueueWork(
        context: Context,
        name: String,
        restart: Boolean,
        repeatInterval: Pair<TimeUnit, Long>,
        flex: Pair<TimeUnit, Long> = repeatInterval
    ){
        val request = PeriodicWorkRequestBuilder<T>(
            repeatInterval.second, repeatInterval.first,
            flex.second, flex.first
        ).addTag(commonTag)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()

        getWorkManager(context).enqueueUniquePeriodicWork(
            name,
            if(restart) ExistingPeriodicWorkPolicy.REPLACE else ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun stopWorker(context: Context, workName: String) = getWorkManager(context).cancelUniqueWork(workName)

    suspend fun startWorkers(mainActivity: MainActivity) {
        startAccountsWorker(mainActivity, false)

        if(CodeforcesNewsLostRecentWorker.isEnabled(mainActivity)) startCodeforcesNewsLostRecentWorker(mainActivity, false)
        if(CodeforcesNewsFollowWorker.isEnabled(mainActivity)) startCodeforcesNewsFollowWorker(mainActivity, false)
        if(ProjectEulerRecentProblemsWorker.isEnabled(mainActivity)) startProjectEulerRecentProblemsWorker(mainActivity, false)

        with(CodeforcesAccountManager(mainActivity).getSettings()){
            if(getContestWatchEnabled()) startCodeforcesContestWatchLauncherWorker(mainActivity, false)
        }
    }

    fun startCodeforcesNewsLostRecentWorker(context: Context, restart: Boolean = true){
        makeAndEnqueueWork<CodeforcesNewsLostRecentWorker>(
            context,
            WorkersNames.codeforces_news_lost_recent,
            restart,
            TimeUnit.HOURS to 1
        )
    }

    fun startCodeforcesNewsFollowWorker(context: Context, restart: Boolean = true){
        makeAndEnqueueWork<CodeforcesNewsFollowWorker>(
            context,
            WorkersNames.codeforces_news_follow,
            restart,
            TimeUnit.HOURS to 6,
            TimeUnit.HOURS to 3,
        )
    }

    fun startCodeforcesContestWatchLauncherWorker(context: Context, restart: Boolean = true){
        makeAndEnqueueWork<CodeforcesContestWatchLauncherWorker>(
            context,
            WorkersNames.codeforces_contest_watch_launcher,
            restart,
            TimeUnit.MINUTES to 45
        )
    }

    fun startAccountsWorker(context: Context, restart: Boolean = true){
        makeAndEnqueueWork<AccountsWorker>(
            context,
            WorkersNames.accounts_parsers,
            restart,
            TimeUnit.MINUTES to 15
        )
    }

    fun startNewsWorker(context: Context, restart: Boolean = true){
        makeAndEnqueueWork<NewsWorker>(
            context,
            WorkersNames.news_parsers,
            restart,
            TimeUnit.HOURS to 6
        )
    }

    fun startProjectEulerRecentProblemsWorker(context: Context, restart: Boolean = true){
        makeAndEnqueueWork<ProjectEulerRecentProblemsWorker>(
            context,
            WorkersNames.project_euler_recent_problems,
            restart,
            TimeUnit.HOURS to 1
        )
    }

}