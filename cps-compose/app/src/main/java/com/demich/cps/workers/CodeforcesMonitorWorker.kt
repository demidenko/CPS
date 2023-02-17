package com.demich.cps.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.demich.cps.NotificationChannels
import com.demich.cps.NotificationIds
import com.demich.cps.R
import com.demich.cps.accounts.managers.CodeforcesAccountManager
import com.demich.cps.contests.monitors.CodeforcesMonitorDataStore
import com.demich.cps.contests.monitors.launchIn
import com.demich.cps.notificationBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CodeforcesMonitorWorker(val context: Context, params: WorkerParameters): CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val monitor = CodeforcesMonitorDataStore(context)

        val contestId = monitor.contestId.flow.filterNotNull().first()
        val handle = monitor.handle()

        val notificationBuilder = createNotificationBuilder(handle, contestId)
        setForeground(ForegroundInfo(NotificationIds.codeforces_contest_monitor, notificationBuilder.build()))

        withContext(Dispatchers.IO) {
            monitor.launchIn(
                scope = this,
                onRatingChange = { ratingChange ->
                    launch { CodeforcesAccountManager(context).applyRatingChange(ratingChange) }
                }
            )
        }

        //TODO: subscribe to monitor data store
        monitor.contestId.flow.takeWhile { it == contestId }.collect()

        return Result.success()
    }

    private fun createNotificationBuilder(handle: String, contestId: Int) =
        notificationBuilder(context, NotificationChannels.codeforces.contest_monitor) {
            setSmallIcon(R.drawable.ic_contest)
            setSubText(handle)
            setShowWhen(false)
            setSilent(true)
            //setStyle(NotificationCompat.DecoratedCustomViewStyle())
            //TODO intent open contest screen
        }
}