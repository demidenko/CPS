package com.demich.cps.workers

import android.content.Context
import androidx.work.WorkerParameters
import com.demich.cps.accounts.managers.CodeforcesAccountManager
import com.demich.cps.accounts.managers.STATUS
import com.demich.cps.utils.codeforces.CodeforcesApi
import com.demich.cps.utils.getCurrentTime
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class CodeforcesMonitorLauncherWorker(
    context: Context,
    parameters: WorkerParameters
): CPSWorker(
    work = getWork(context),
    parameters = parameters
) {
    companion object {
        fun getWork(context: Context) = object : CPSWork(name = "cf_monitor_launcher", context = context) {
            override suspend fun isEnabled() = CodeforcesAccountManager(context).getSettings().monitorEnabled()
            override val requestBuilder get() =
                CPSPeriodicWorkRequestBuilder<CodeforcesMonitorLauncherWorker>(
                    repeatInterval = 45.minutes
                )
        }
    }

    override suspend fun runWork(): Result {
        val manager = CodeforcesAccountManager(context)

        val info = manager.getSavedInfo()
        if (info.status != STATUS.OK) return Result.success()

        val currentTime = getCurrentTime()
        fun isTooLate(time: Instant) = currentTime - time > 24.hours

        val (lastKnownId, canceledIds) = with(manager.getSettings()) {
            monitorCanceledContests.updateValue { list ->
                list.filter { !isTooLate(it.second) }
            }
            Pair(
                first = monitorLastSubmissionId(),
                second = monitorCanceledContests().map { it.first }
            )
        }


        //TODO
        return Result.success()
    }

}