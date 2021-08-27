package com.example.test3.workers

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.example.test3.*
import com.example.test3.news.settingsNews
import com.example.test3.room.getFollowDao
import com.example.test3.utils.CodeforcesBlogEntry
import com.example.test3.utils.CodeforcesURLFactory
import com.example.test3.utils.fromHTML
import java.util.concurrent.TimeUnit

class CodeforcesNewsFollowWorker(private val context: Context, val params: WorkerParameters): CoroutineWorker(context, params) {
    companion object {
        suspend fun isEnabled(context: Context): Boolean = context.settingsNews.followEnabled()
    }

    override suspend fun doWork(): Result {

        if(!isEnabled(context)){
            WorkersCenter.stopWorker(context, WorkersNames.codeforces_news_follow)
            return Result.success()
        }


        setForeground(ForegroundInfo(
            NotificationIDs.codeforces_follow_progress,
            createProgressNotification().setProgress(100,0,true).build()
        ))

        val dao = getFollowDao(context)
        val notificationManagerCompat = NotificationManagerCompat.from(context)

        val savedHandles = dao.getHandles().shuffled()
        savedHandles.forEachIndexed { index, handle ->
            dao.loadBlogEntries(handle, context)
            notificationManagerCompat.notify(
                NotificationIDs.codeforces_follow_progress,
                createProgressNotification().setProgress(savedHandles.size, index+1, false).build()
            )
        }

        return Result.success()
    }

    private fun createProgressNotification(): NotificationCompat.Builder {
        return notificationBuilder(context, NotificationChannels.codeforces_follow_progress) {
            setContentTitle("Codeforces Follow Update...")
            setSmallIcon(R.drawable.ic_logo_codeforces)
            setSilent(true)
            setShowWhen(false)
        }
    }

}

fun notifyNewBlogEntry(blogEntry: CodeforcesBlogEntry, context: Context){
    val title = fromHTML(blogEntry.title.removeSurrounding("<p>", "</p>")).toString()
    notificationBuildAndNotify(
        context,
        NotificationChannels.codeforces_follow_new_blog,
        NotificationIDs.makeCodeforcesFollowBlogID(blogEntry.id)
    ) {
        setSubText("New codeforces blog entry")
        setContentTitle(blogEntry.authorHandle)
        setBigContent(title)
        setSmallIcon(R.drawable.ic_new_post)
        setAutoCancel(true)
        setShowWhen(true)
        setWhen(TimeUnit.SECONDS.toMillis(blogEntry.creationTimeSeconds))
        setContentIntent(makePendingIntentOpenURL(CodeforcesURLFactory.blog(blogEntry.id), context))
    }
}