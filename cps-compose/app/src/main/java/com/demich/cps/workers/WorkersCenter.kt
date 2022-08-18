package com.demich.cps.workers

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.lifecycle.asFlow
import androidx.lifecycle.map
import androidx.work.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

private val Context.workManager get() = WorkManager.getInstance(this)

private const val commonTag = "cps_worker"

abstract class CPSWork(
    val name: String,
    val context: Context
) {
    abstract suspend fun isEnabled(): Boolean

    abstract val requestBuilder: PeriodicWorkRequest.Builder

    fun stop() {
        context.workManager.cancelUniqueWork(name)
    }

    private fun start(restart: Boolean) {
        val request = requestBuilder.apply {
            addTag(commonTag)
            setBackoffCriteria(
                BackoffPolicy.LINEAR,
                PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS,
                TimeUnit.MILLISECONDS
            )
        }.build()

        context.workManager.enqueueUniquePeriodicWork(
            name,
            if (restart) ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE else ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun startImmediate() = start(restart = true)
    fun enqueue() = start(restart = false)
    suspend fun enqueueIfEnabled() {
        if (isEnabled()) enqueue()
    }

    fun flowOfInfo(): Flow<WorkInfo?> =
        context.workManager.getWorkInfosForUniqueWorkLiveData(name)
            .asFlow()
            .map { it?.getOrNull(0) }
}

@Composable
fun CPSWork.workInfoState(): State<WorkInfo?> = remember(this) {
        context.workManager.getWorkInfosForUniqueWorkLiveData(name)
            .map { it?.getOrNull(0) }
    }.observeAsState()

inline fun<reified W: CPSWorker> CPSPeriodicWorkRequestBuilder(
    repeatInterval: Duration,
    flex: Duration = repeatInterval,
    batteryNotLow: Boolean = false
) = PeriodicWorkRequestBuilder<W>(
    repeatInterval = repeatInterval.inWholeMilliseconds,
    repeatIntervalTimeUnit = TimeUnit.MILLISECONDS,
    flexTimeInterval = flex.inWholeMilliseconds,
    flexTimeIntervalUnit = TimeUnit.MILLISECONDS
).setConstraints(
    Constraints(
        requiredNetworkType = NetworkType.CONNECTED,
        requiresBatteryNotLow = batteryNotLow,
        requiresCharging = false
    )
)

fun Context.getCPSWorks() = listOf(
    AccountsWorker::getWork,
    ContestsWorker::getWork,
    CodeforcesNewsFollowWorker::getWork,
    CodeforcesNewsLostRecentWorker::getWork,
    CodeforcesUpsolvingSuggestionsWorker::getWork
).map { it(this) }

suspend fun Context.enqueueEnabledWorkers() {
    getCPSWorks().forEach { it.enqueueIfEnabled() }
}

/*
object WorkersCenter {

    fun startCodeforcesContestWatchLauncherWorker(context: Context, restart: Boolean = true) {
        makeAndEnqueueWork<CodeforcesContestWatchLauncherWorker>(
            context,
            WorkersNames.codeforces_contest_watch_launcher,
            restart,
            45.minutes
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