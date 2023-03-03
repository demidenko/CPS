package com.demich.cps.workers

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.work.*
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.toJavaDuration

internal val Context.workManager get() = WorkManager.getInstance(this)


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
    private fun enqueue() = start(restart = false)
    suspend fun enqueueIfEnabled() {
        if (isEnabled()) enqueue()
    }

    fun workInfoLiveData(): LiveData<WorkInfo?> =
        context.workManager.getWorkInfosForUniqueWorkLiveData(name)
            .map { it?.getOrNull(0) }
}

internal inline fun<reified W: CPSWorker> CPSPeriodicWorkRequestBuilder(
    repeatInterval: Duration,
    flex: Duration = repeatInterval,
    batteryNotLow: Boolean = false
) = PeriodicWorkRequestBuilder<W>(
    repeatInterval = repeatInterval.toJavaDuration(),
    flexTimeInterval = flex.toJavaDuration()
).setConstraints(
    Constraints(
        requiredNetworkType = NetworkType.CONNECTED,
        requiresBatteryNotLow = batteryNotLow,
        requiresCharging = false
    )
)

fun Context.getCPSWorks() = listOf(
    AccountsWorker::getWork,
    NewsWorker::getWork,
    ContestsWorker::getWork,
    CodeforcesNewsFollowWorker::getWork,
    CodeforcesNewsLostRecentWorker::getWork,
    CodeforcesMonitorLauncherWorker::getWork,
    CodeforcesUpsolvingSuggestionsWorker::getWork,
    ProjectEulerRecentProblemsWorker::getWork,
    UtilityWorker::getWork
).map { it(this) }

suspend fun Context.enqueueEnabledWorkers() {
    getCPSWorks().forEach { it.enqueueIfEnabled() }
}

internal fun WorkManager.enqueueCodeforcesMonitorWorker(replace: Boolean) {
    enqueueUniqueWork(
        "cf_monitor",
        if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
        OneTimeWorkRequestBuilder<CodeforcesMonitorWorker>().build()
    )
}