package com.example.test3.workers

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.example.test3.*
import com.example.test3.account_manager.STATUS
import com.example.test3.news.settingsNews
import com.example.test3.room.UserBlogs
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
        suspend fun getBlogEntries(handle: String) = dao.getUserBlogs(handle)?.blogs

        suspend fun add(handle: String): Boolean {
            if(dao.getUserBlogs(handle)!=null) return false

            dao.insert(
                UserBlogs(
                    handle = handle,
                    blogs = null
                )
            )

            val locale = NewsFragment.getCodeforcesContentLanguage(context)
            val userBlogs = CodeforcesAPI.getUserBlogEntries(handle,locale)?.result?.map { it.id }

            setBlogEntries(handle, userBlogs)

            return true
        }

        suspend fun remove(handle: String){
            dao.remove(handle)
        }

        suspend fun changeHandle(fromHandle: String, toHandle: String){
            if(fromHandle == toHandle) return
            val fromUserBlogs = dao.getUserBlogs(fromHandle) ?: return
            dao.getUserBlogs(toHandle)?.let { toUserBlogs ->
                if(toUserBlogs.id != fromUserBlogs.id){
                    dao.remove(fromHandle)
                    return
                }
            }
            dao.update(fromUserBlogs.copy(handle = toHandle))
        }

        suspend fun setBlogEntries(handle: String, blogs: List<Int>?){
            val userBlogs = dao.getUserBlogs(handle) ?: return
            dao.update(userBlogs.copy(blogs = blogs))
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

        val proceeded = mutableSetOf<String>()
        suspend fun proceedUser(handle: String){
            if(!proceeded.add(handle.toLowerCase())) return

            val response = CodeforcesAPI.getUserBlogEntries(handle, locale) ?: return
            if(response.status == CodeforcesAPIStatus.FAILED){
                //"handle: You are not allowed to read that blog" -> no activity
                if(response.isBlogHandleNotFound(handle)){
                    val (realHandle, status) = CodeforcesUtils.getRealHandle(handle)
                    when(status){
                        STATUS.OK -> {
                            connector.changeHandle(handle, realHandle)
                            proceedUser(realHandle)
                            return
                        }
                        STATUS.NOT_FOUND -> connector.remove(handle)
                        STATUS.FAILED -> return
                    }
                }
                return
            }

            val result = response.result ?: return

            var hasNewBlog = false
            val saved = connector.getBlogEntries(handle)?.toSet()

            if(saved == null){
                hasNewBlog = true
            }else{
                result.forEach { blogEntry ->
                    if(blogEntry.id !in saved){
                        hasNewBlog = true
                        notifyNewBlog(blogEntry)
                    }
                }
            }
            if(hasNewBlog){
                connector.setBlogEntries(handle, result.map { it.id })
            }
        }

        val notificationManagerCompat = NotificationManagerCompat.from(context)
        savedHandles.forEachIndexed { index, handle ->
            proceedUser(handle)
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

    private fun notifyNewBlog(blogEntry: CodeforcesBlogEntry){
        val title = fromHTML(blogEntry.title.removeSurrounding("<p>", "</p>")).toString()
        val n = notificationBuilder(context, NotificationChannels.codeforces_follow_new_blog).apply {
            setSubText("New codeforces blog")
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
}