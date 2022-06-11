package com.demich.cps.workers

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

private val Context.workManager get() = WorkManager.getInstance(this)

private const val commonTag = "cps_worker"

abstract class WorkerCarrier(
    val context: Context,
    val name: String
) {
    abstract suspend fun isEnabled(): Boolean

    abstract val requestBuilder: PeriodicWorkRequest.Builder

    fun stop() {
        context.workManager.cancelUniqueWork(name)
    }

    protected fun start(restart: Boolean) {
        enqueuePeriodicWork(
            restart = restart,
            builder = requestBuilder
        )
    }

    fun enqueue() = start(restart = false)
    fun startImmediate() = start(restart = true)
}

inline fun<reified W: CPSWorker> PeriodicWorkRequestBuilder(
    repeatInterval: Duration,
    flex: Duration = repeatInterval,
    batteryNotLow: Boolean = false
) = PeriodicWorkRequestBuilder<W>(
    repeatInterval = repeatInterval.inWholeMilliseconds,
    repeatIntervalTimeUnit = TimeUnit.MILLISECONDS,
    flexTimeInterval = flex.inWholeMilliseconds,
    flexTimeIntervalUnit = TimeUnit.MILLISECONDS
).apply {
    setConstraints(
        Constraints(
            requiredNetworkType = NetworkType.CONNECTED,
            requiresBatteryNotLow = batteryNotLow,
            requiresCharging = false
        )
    )
}

private fun WorkerCarrier.enqueuePeriodicWork(
    restart: Boolean,
    builder: PeriodicWorkRequest.Builder
) {
    val request = builder.apply {
        addTag(commonTag)
        setBackoffCriteria(
            BackoffPolicy.LINEAR,
            PeriodicWorkRequest.MIN_BACKOFF_MILLIS,
            TimeUnit.MILLISECONDS
        )
    }.build()

    context.workManager.enqueueUniquePeriodicWork(
        name,
        if (restart) ExistingPeriodicWorkPolicy.REPLACE else ExistingPeriodicWorkPolicy.KEEP,
        request
    )
}



/*fun flowOfWorkInfo(context: Context, name: String): Flow<WorkInfo> =
    context.workManager.getWorkInfosForUniqueWorkLiveData(name).asFlow()
        .map { it?.getOrNull(0) }.filterNotNull()
*/

suspend fun Context.startWorkers() {
    with(CodeforcesNewsFollowWorker.getCarrier(this)) {
        if (isEnabled()) enqueue()
    }
}

/*
object WorkersCenter {

    fun startCodeforcesNewsLostRecentWorker(context: Context, restart: Boolean = true) {
        makeAndEnqueueWork<CodeforcesNewsLostRecentWorker>(
            context,
            WorkersNames.codeforces_news_lost_recent,
            restart,
            1.hours
        )
    }

    fun startCodeforcesContestWatchLauncherWorker(context: Context, restart: Boolean = true) {
        makeAndEnqueueWork<CodeforcesContestWatchLauncherWorker>(
            context,
            WorkersNames.codeforces_contest_watch_launcher,
            restart,
            45.minutes
        )
    }

    fun startCodeforcesUpsolveSuggestionsWorker(context: Context, restart: Boolean = true) {
        makeAndEnqueueWork<CodeforcesUpsolvingSuggestionsWorker>(
            context,
            WorkersNames.codeforces_upsolving_suggestions,
            restart,
            12.hours,
            batteryNotLow = true
        )
    }

    fun startAccountsWorker(context: Context, restart: Boolean = true) {
        makeAndEnqueueWork<AccountsWorker>(
            context,
            WorkersNames.accounts_parsers,
            restart,
            15.minutes
        )
    }

    fun startNewsWorker(context: Context, restart: Boolean = true) {
        makeAndEnqueueWork<NewsWorker>(
            context,
            WorkersNames.news_parsers,
            restart,
            6.hours,
            batteryNotLow = true
        )
    }

    fun startProjectEulerRecentProblemsWorker(context: Context, restart: Boolean = true) {
        makeAndEnqueueWork<ProjectEulerRecentProblemsWorker>(
            context,
            WorkersNames.project_euler_recent_problems,
            restart,
            1.hours
        )
    }

    fun startCodeforcesContestWatcher(context: Context, request: OneTimeWorkRequest.Builder) {
        getWorkManager(context).enqueueUniqueWork(
            WorkersNames.codeforces_contest_watcher,
            ExistingWorkPolicy.REPLACE,
            request.addTag(commonTag).build()
        )
    }
}*/