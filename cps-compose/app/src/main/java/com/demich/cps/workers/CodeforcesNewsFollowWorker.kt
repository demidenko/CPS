package com.demich.cps.workers

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.demich.cps.*
import com.demich.cps.news.settings.settingsNews
import com.demich.cps.room.followListDao
import kotlin.time.Duration.Companion.hours


class CodeforcesNewsFollowWorker(
    context: Context,
    parameters: WorkerParameters
): CPSWorker(
    carrier = getCarrier(context),
    parameters = parameters
) {
    companion object {
        fun getCarrier(context: Context) = object : WorkerCarrier(name = "cf_follow", context = context) {
            override suspend fun isEnabled() = context.settingsNews.codeforcesFollowEnabled()
            override val requestBuilder get() =
                PeriodicWorkRequestBuilder<CodeforcesNewsFollowWorker>(
                    repeatInterval = 6.hours,
                    flex = 3.hours,
                    batteryNotLow = true
                )

        }
    }

    override suspend fun runWork(): Result {
        setForeground(ForegroundInfo(
            NotificationIds.codeforces_follow_progress,
            progressNotificationBuilder(total = 0, done = 0).build()
        ))

        val dao = context.followListDao
        val notificationManagerCompat = NotificationManagerCompat.from(context)

        val savedHandles = dao.getHandles().shuffled()
        savedHandles.forEachIndexed { index, handle ->
            dao.getAndReloadBlogEntries(handle, context)
            progressNotificationBuilder(total = savedHandles.size, done = index+1)
                .notifyBy(notificationManagerCompat, NotificationIds.codeforces_follow_progress)
        }

        return Result.success()
    }

    private fun progressNotificationBuilder(total: Int, done: Int) =
        notificationBuilder(context, NotificationChannels.codeforces.follow_progress) {
            setContentTitle("Codeforces Follow Update...")
            setSmallIcon(R.drawable.ic_logo_codeforces)
            setSilent(true)
            setShowWhen(false)
            setProgress(total, done, total == 0)
        }
}
