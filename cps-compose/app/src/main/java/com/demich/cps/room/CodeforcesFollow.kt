package com.demich.cps.room

import android.content.Context
import com.demich.cps.*
import com.demich.cps.accounts.userinfo.CodeforcesUserInfo
import com.demich.cps.accounts.userinfo.STATUS
import com.demich.cps.features.codeforces.follow.database.CodeforcesFollowDao
import com.demich.cps.features.codeforces.follow.database.CodeforcesUserBlog
import com.demich.cps.features.codeforces.follow.database.cfFollowDao
import com.demich.cps.news.settings.settingsNews
import com.demich.cps.platforms.api.CodeforcesApi
import com.demich.cps.platforms.api.CodeforcesBlogEntry
import com.demich.cps.platforms.utils.CodeforcesUtils

val Context.followListDao: FollowListDao
    get() = FollowListDao(
        context = this,
        dao = this.cfFollowDao
    )

class FollowListDao internal constructor(
    private val context: Context,
    private val dao: CodeforcesFollowDao
) {
    suspend fun remove(handle: String) = dao.remove(handle)

    fun flowOfAllBlogs() = dao.flowOfAllBlogs()

    suspend fun getHandles() = dao.getHandles()

    suspend fun getAndReloadBlogEntries(handle: String): List<CodeforcesBlogEntry>? {
        val settingsNews = context.settingsNews
        val blogEntries = dao.getAndReloadBlogEntries(
            handle = handle,
            locale = settingsNews.codeforcesLocale(),
            onNewBlogEntry = ::notifyNewBlogEntry
        )

        return blogEntries
    }

    suspend fun addNewUser(userInfo: CodeforcesUserInfo) {
        if (dao.getUserBlog(userInfo.handle) != null) return
        dao.insert(
            CodeforcesUserBlog(
                handle = userInfo.handle,
                blogEntries = null,
                userInfo = userInfo
            )
        )
        getAndReloadBlogEntries(handle = userInfo.handle)
    }

    suspend fun addNewUser(handle: String) {
        if (dao.getUserBlog(handle) != null) return
        //TODO: sync?? parallel? (addNewUser loads blog without info)
        addNewUser(userInfo = CodeforcesUserInfo(handle = handle, status = STATUS.FAILED))
        dao.applyUserInfo(
            handle = handle,
            info = CodeforcesUtils.getUserInfo(handle = handle, doRedirect = true)
        )
    }

    suspend fun updateUsers() {
        dao.updateUsersInfo()
        dao.getAllBlogs().forEach {
            if (it.blogEntries == null) getAndReloadBlogEntries(handle = it.handle)
        }
    }

    private fun notifyNewBlogEntry(blogEntry: CodeforcesBlogEntry) =
        notificationBuildAndNotify(
            context = context,
            channel = NotificationChannels.codeforces.follow_new_blog,
            notificationId = NotificationIds.makeCodeforcesFollowBlogId(blogEntry.id)
        ) {
            setSubText("New codeforces blog entry")
            setContentTitle(blogEntry.authorHandle)
            setBigContent(CodeforcesUtils.extractTitle(blogEntry))
            setSmallIcon(R.drawable.ic_new_post)
            setAutoCancel(true)
            setWhen(blogEntry.creationTime)
            attachUrl(url = CodeforcesApi.urls.blogEntry(blogEntry.id), context = context)
        }
}