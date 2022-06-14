package com.demich.cps.workers

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.demich.cps.*
import com.demich.cps.news.settings.settingsNews
import com.demich.cps.room.followListDao
import com.demich.cps.ui.bottomprogressbar.ProgressBarInfo
import kotlin.time.Duration.Companion.hours


class CodeforcesNewsFollowWorker(
    context: Context,
    parameters: WorkerParameters
): CPSWorker(
    work = getWork(context),
    parameters = parameters
) {
    companion object {
        fun getWork(context: Context) = object : CPSWork(name = "cf_follow", context = context) {
            override suspend fun isEnabled() = context.settingsNews.codeforcesFollowEnabled()
            override val requestBuilder get() =
                CPSPeriodicWorkRequestBuilder<CodeforcesNewsFollowWorker>(
                    repeatInterval = 6.hours,
                    //flex = 3.hours,
                    batteryNotLow = true
                )

        }
    }

    override suspend fun runWork(): Result {
        setForeground(ForegroundInfo(
            NotificationIds.codeforces_follow_progress,
            progressNotificationBuilder().build()
        ))

        val dao = context.followListDao
        val notificationManagerCompat = NotificationManagerCompat.from(context)

        val savedHandles = dao.getHandles().shuffled()
        savedHandles.forEachIndexed { index, handle ->
            if (dao.getAndReloadBlogEntries(handle, context) == null) return Result.retry()

            val progressDone = index + 1

            progressNotificationBuilder()
                .setProgress(savedHandles.size, progressDone, false)
                .notifyBy(notificationManagerCompat, NotificationIds.codeforces_follow_progress)

            setProgressInfo(ProgressBarInfo(total = savedHandles.size, current = progressDone))
        }

        return Result.success()
    }

    private fun progressNotificationBuilder() =
        notificationBuilder(context, NotificationChannels.codeforces.follow_progress) {
            setContentTitle("Codeforces follow update...")
            setSmallIcon(R.drawable.ic_logo_codeforces)
            setSilent(true)
            setShowWhen(false)
        }
}
