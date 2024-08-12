package com.demich.cps.workers

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.*
import com.demich.cps.notifications.NotificationBuilder
import com.demich.cps.utils.getCurrentTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

internal val Context.workManager get() = WorkManager.getInstance(this)


abstract class CPSWork(
    val name: String,
    val context: Context
) {
    fun stop() {
        context.workManager.cancelUniqueWork(name)
    }

    fun flowOfWorkInfo(): Flow<WorkInfo?> =
        context.workManager.getWorkInfosForUniqueWorkFlow(name)
            .map { it?.getOrNull(0) }
}

abstract class CPSOneTimeWork(
    name: String,
    context: Context
): CPSWork(name, context) {
    abstract val requestBuilder: OneTimeWorkRequest.Builder

    fun enqueue(replace: Boolean) {
        val request = requestBuilder.build()

        context.workManager.enqueueUniqueWork(
            name,
            if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            request
        )
    }
}

abstract class CPSPeriodicWork(
    name: String,
    context: Context
): CPSWork(name, context) {
    abstract suspend fun isEnabled(): Boolean

    abstract val requestBuilder: PeriodicWorkRequest.Builder

    private fun start(restart: Boolean) {
        val request = requestBuilder.build()

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
    fun enqueueRetry() {
        val request = requestBuilder.apply {
            setNextScheduleTimeOverride((getCurrentTime() + 15.minutes).toEpochMilliseconds())
        }.build()

        context.workManager.enqueueUniquePeriodicWork(
            name,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}

internal inline fun<reified W: CPSWorker> CPSPeriodicWorkRequestBuilder(
    repeatInterval: Duration,
    flex: Duration = repeatInterval,
    batteryNotLow: Boolean = false,
    requiresCharging: Boolean = false,
    requireNetwork: Boolean = true
) = PeriodicWorkRequestBuilder<W>(
    repeatInterval = repeatInterval.toJavaDuration(),
    flexTimeInterval = flex.toJavaDuration()
).setConstraints(
    Constraints(
        requiredNetworkType = if (requireNetwork) NetworkType.CONNECTED else NetworkType.NOT_REQUIRED,
        requiresBatteryNotLow = batteryNotLow,
        requiresCharging = requiresCharging
    )
).apply {
    setBackoffCriteria(
        BackoffPolicy.LINEAR,
        PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS,
        TimeUnit.MILLISECONDS
    )
}

fun Context.getCPSWorks() = listOf(
    AccountsWorker::getWork,
    NewsWorker::getWork,
    ContestsWorker::getWork,
    CodeforcesCommunityFollowWorker::getWork,
    CodeforcesCommunityLostRecentWorker::getWork,
    CodeforcesMonitorLauncherWorker::getWork,
    CodeforcesUpsolvingSuggestionsWorker::getWork,
    ProjectEulerRecentProblemsWorker::getWork,
    UtilityWorker::getWork
).map { it(this) }

suspend fun Context.enqueueEnabledWorkers() {
    getCPSWorks().forEach { it.enqueueIfEnabled() }
}

//TODO: move to worker
internal fun getCodeforcesMonitorWork(context: Context): CPSOneTimeWork =
    object : CPSOneTimeWork(name = "cf_monitor", context = context) {
        override val requestBuilder: OneTimeWorkRequest.Builder
            get() = OneTimeWorkRequestBuilder<CodeforcesMonitorWorker>()
    }

internal suspend fun CoroutineWorker.setForeground(builder: NotificationBuilder) {
    setForeground(ForegroundInfo(
        builder.notificationId,
        builder.build(),
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0
    ))
}