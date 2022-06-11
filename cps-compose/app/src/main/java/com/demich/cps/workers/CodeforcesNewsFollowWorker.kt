package com.demich.cps.workers

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.demich.cps.*
import com.demich.cps.news.settings.settingsNews
import com.demich.cps.room.followListDao


class CodeforcesNewsFollowWorker(private val context: Context, val params: WorkerParameters): CoroutineWorker(context, params) {
    companion object {
        suspend fun isEnabled(context: Context): Boolean = context.settingsNews.codeforcesFollowEnabled()
    }

    override suspend fun doWork(): Result {
        if (!isEnabled(context)) {
            WorkersCenter.stopWorker(context, WorkersNames.codeforces_news_follow)
            return Result.success()
        }

        setForeground(ForegroundInfo(
            NotificationIds.codeforces_follow_progress,
            createProgressNotificationBuilder(total = 0, done = 0).build()
        ))

        val dao = context.followListDao
        val notificationManagerCompat = NotificationManagerCompat.from(context)

        val savedHandles = dao.getHandles().shuffled()
        savedHandles.forEachIndexed { index, handle ->
            dao.getAndReloadBlogEntries(handle, context)
            createProgressNotificationBuilder(total = savedHandles.size, done = index+1)
                .notifyBy(notificationManagerCompat, NotificationIds.codeforces_follow_progress)
        }

        return Result.success()
    }

    private fun createProgressNotificationBuilder(total: Int, done: Int) =
        notificationBuilder(context, NotificationChannels.codeforces.follow_progress) {
            setContentTitle("Codeforces Follow Update...")
            setSmallIcon(R.drawable.ic_logo_codeforces)
            setSilent(true)
            setShowWhen(false)
            setProgress(total, done, total == 0)
        }
}
