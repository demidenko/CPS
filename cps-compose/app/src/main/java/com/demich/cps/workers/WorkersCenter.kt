package com.demich.cps.workers

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.demich.cps.notifications.NotificationBuilder
import com.demich.cps.utils.getCurrentTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration


private val Context.workManager: WorkManager
    get() = WorkManager.getInstance(this)

abstract class CPSWork(
    val name: String,
    val context: Context
) {
    val workManager: WorkManager get() = context.workManager

    fun stop() {
        workManager.cancelUniqueWork(name)
    }

    fun flowOfWorkInfo(): Flow<WorkInfo?> =
        workManager.getWorkInfosForUniqueWorkFlow(name)
            .map { it.firstOrNull() }
}

abstract class CPSOneTimeWork(
    name: String,
    context: Context
): CPSWork(name, context) {
    abstract val requestBuilder: OneTimeWorkRequest.Builder

    private inline fun enqueueWork(
        policy: ExistingWorkPolicy,
        block: OneTimeWorkRequest.Builder.() -> Unit = {}
    ) {
        workManager.enqueueUniqueWork(
            uniqueWorkName = name,
            existingWorkPolicy = policy,
            request = requestBuilder.apply(block).build()
        )
    }

    fun enqueue(replace: Boolean) =
        enqueueWork(policy = if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP)
}

abstract class CPSPeriodicWork(
    name: String,
    context: Context
): CPSWork(name, context) {
    abstract suspend fun isEnabled(): Boolean

    abstract suspend fun requestBuilder(): PeriodicWorkRequest.Builder

    private suspend inline fun enqueueWork(
        policy: ExistingPeriodicWorkPolicy,
        block: PeriodicWorkRequest.Builder.() -> Unit = {}
    ) {
        workManager.enqueueUniquePeriodicWork(
            uniqueWorkName = name,
            existingPeriodicWorkPolicy = policy,
            request = requestBuilder().apply(block).build()
        )
    }

    private suspend fun start(restart: Boolean) =
        enqueueWork(
            policy = if (restart) ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
                    else ExistingPeriodicWorkPolicy.UPDATE
        )

    suspend fun startImmediate() = start(restart = true)

    private suspend fun enqueue() = start(restart = false)

    suspend fun enqueueIfEnabled() {
        if (isEnabled()) enqueue()
    }

    private suspend fun getWorkInfo(): WorkInfo? = flowOfWorkInfo().firstOrNull()

    private suspend fun enqueueAt(time: Instant) {
        if (!isEnabled()) return
        enqueueWork(policy = ExistingPeriodicWorkPolicy.UPDATE) {
            //note: ignores default even if default closer
            setNextScheduleTimeOverride(time)
        }
    }

    suspend fun enqueueAtIfEarlier(time: Instant) {
        getWorkInfo()?.repeatInterval?.let {
            if (getCurrentTime() + it < time) return
        }
        enqueueAt(time)
    }

    suspend fun enqueueInRepeatInterval() {
        getWorkInfo()?.repeatInterval?.let {
            enqueueAt(time = getCurrentTime() + it)
        }
    }

    suspend fun enqueueAsap() {
        val time = getCurrentTime() + PeriodicWorkRequest.minPeriodicInterval
        getWorkInfo()?.nextScheduleTime?.let {
            if (it < time) return
        }
        enqueueAt(time = time)
    }
}

internal inline fun<reified W: CPSWorker> CPSPeriodicWorkRequestBuilder(
    repeatInterval: Duration,
    flex: Duration = repeatInterval,
    batteryNotLow: Boolean = false,
    requiresCharging: Boolean = false,
    requireNetwork: Boolean = true
) =
    PeriodicWorkRequestBuilder<W>(
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
            backoffPolicy = BackoffPolicy.LINEAR,
            duration = PeriodicWorkRequest.minPeriodicInterval.toJavaDuration()
        )
    }

fun Context.getCPSWorks() = listOf(
    ProfilesWorker::getWork,
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
    //accounts renamed to profiles but work still in database
    //TODO: get all enqueued (somehow) and cancel not from list
    workManager.cancelUniqueWork(uniqueWorkName = "accounts")

    getCPSWorks().forEach { it.enqueueIfEnabled() }
}

internal suspend fun CoroutineWorker.setForeground(builder: NotificationBuilder) {
    setForeground(ForegroundInfo(
        builder.notificationId,
        builder.build(),
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0
    ))
}


private val PeriodicWorkRequest.Companion.minPeriodicInterval: Duration
    get() = MIN_PERIODIC_INTERVAL_MILLIS.milliseconds

private fun PeriodicWorkRequest.Builder.setNextScheduleTimeOverride(instant: Instant) =
    setNextScheduleTimeOverride(instant.toEpochMilliseconds())

val WorkInfo?.stateOrCancelled: WorkInfo.State
    get() = this?.state ?: WorkInfo.State.CANCELLED

val WorkInfo?.isRunning: Boolean
    get() = this?.state == WorkInfo.State.RUNNING

val WorkInfo.repeatInterval: Duration?
    get() = periodicityInfo?.repeatIntervalMillis?.milliseconds

val WorkInfo.nextScheduleTime: Instant?
    get() = when (state) {
        WorkInfo.State.ENQUEUED -> Instant.fromEpochMilliseconds(nextScheduleTimeMillis)
        else -> null
    }