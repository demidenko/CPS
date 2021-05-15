package com.example.test3.workers

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.example.test3.*
import com.example.test3.account_manager.CodeforcesUserInfo
import com.example.test3.account_manager.STATUS
import com.example.test3.news.settingsNews
import com.example.test3.room.getFollowDao
import com.example.test3.utils.*
import java.util.concurrent.TimeUnit

class CodeforcesNewsFollowWorker(private val context: Context, val params: WorkerParameters): CoroutineWorker(context, params) {
    companion object {
        suspend fun isEnabled(context: Context): Boolean = context.settingsNews.getFollowEnabled()
    }

    class FollowDataConnector(private val context: Context) {

        private val dao by lazy { getFollowDao(context) }

        suspend fun getHandles(): List<String> = dao.getAll().sortedByDescending { it.id }.map { it.handle }
        suspend fun getBlogEntries(handle: String) = dao.getUserBlog(handle)?.blogEntries

        private suspend fun changeHandle(fromHandle: String, toHandle: String){
            if(fromHandle == toHandle) return
            val fromUserBlog = dao.getUserBlog(fromHandle) ?: return
            dao.getUserBlog(toHandle)?.let { toUserBlog ->
                if(toUserBlog.id != fromUserBlog.id){
                    dao.remove(fromHandle)
                    return
                }
            }
            dao.update(fromUserBlog.copy(handle = toHandle))
        }

        private suspend fun setUserInfo(handle: String, info: CodeforcesUserInfo) {
            if(info.status != STATUS.OK) return
            if(info.handle != handle) changeHandle(handle, info.handle)
            val userBlog = dao.getUserBlog(info.handle) ?: return
            if(userBlog.userInfo != info) dao.update(userBlog.copy(userInfo = info))
        }

        suspend fun loadBlogEntries(handle: String) = loadBlogEntries(handle, NewsFragment.getCodeforcesContentLanguage(context))

        suspend fun loadBlogEntries(handle: String, locale: CodeforcesLocale): List<CodeforcesBlogEntry> {
            val response = CodeforcesAPI.getUserBlogEntries(handle, locale) ?: return emptyList()
            if(response.status == CodeforcesAPIStatus.FAILED){
                //"handle: You are not allowed to read that blog" -> no activity
                if(response.isBlogHandleNotFound(handle)){
                    val (realHandle, status) = CodeforcesUtils.getRealHandle(handle)
                    return when(status){
                        STATUS.OK -> {
                            changeHandle(handle, realHandle)
                            loadBlogEntries(realHandle, locale)
                        }
                        STATUS.NOT_FOUND -> {
                            dao.remove(handle)
                            emptyList()
                        }
                        STATUS.FAILED -> emptyList()
                    }
                }
                return emptyList()
            }
            val result = response.result ?: return emptyList()
            val updated =
                getBlogEntries(handle)?.toSet()?.let { saved ->
                    result.filter { it.id !in saved }
                        .onEach { blogEntry ->
                            notifyNewBlogEntry(blogEntry, context)
                        }.isNotEmpty()
                } ?: true
            if(updated) dao.setBlogEntries(handle, result.map { it.id })
            return result
        }

        suspend fun updateUsersInfo() {
            CodeforcesUtils.getUsersInfo(getHandles(), true)
                .forEach { (handle, info) ->
                    when (info.status) {
                        STATUS.NOT_FOUND -> dao.remove(handle)
                        STATUS.OK -> setUserInfo(handle, info)
                    }
                }
        }

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

        val connector = FollowDataConnector(context)
        val savedHandles = connector.getHandles()
        val locale = NewsFragment.getCodeforcesContentLanguage(context)

        val notificationManagerCompat = NotificationManagerCompat.from(context)
        savedHandles.forEachIndexed { index, handle ->
            connector.loadBlogEntries(handle, locale)
            notificationManagerCompat.notify(
                NotificationIDs.codeforces_follow_progress,
                createProgressNotification().setProgress(savedHandles.size, index+1, false).build()
            )
        }

        return Result.success()
    }

    private fun createProgressNotification(): NotificationCompat.Builder {
        return notificationBuilder(context, NotificationChannels.codeforces_follow_progress)
            .setContentTitle("Codeforces Follow Update...")
            .setSmallIcon(R.drawable.ic_cf_logo)
            .setNotificationSilent()
            .setShowWhen(false)
    }

}

private fun notifyNewBlogEntry(blogEntry: CodeforcesBlogEntry, context: Context){
    val title = fromHTML(blogEntry.title.removeSurrounding("<p>", "</p>")).toString()
    val n = notificationBuilder(context, NotificationChannels.codeforces_follow_new_blog).apply {
        setSubText("New codeforces blog entry")
        setContentTitle(blogEntry.authorHandle)
        setBigContent(title)
        setSmallIcon(R.drawable.ic_new_post)
        setAutoCancel(true)
        setShowWhen(true)
        setWhen(TimeUnit.SECONDS.toMillis(blogEntry.creationTimeSeconds))
        setContentIntent(makePendingIntentOpenURL(CodeforcesURLFactory.blog(blogEntry.id), context))
    }
    NotificationManagerCompat.from(context).notify(NotificationIDs.makeCodeforcesFollowBlogID(blogEntry.id), n.build())
}