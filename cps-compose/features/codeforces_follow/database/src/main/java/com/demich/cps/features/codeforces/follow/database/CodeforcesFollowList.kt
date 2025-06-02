package com.demich.cps.features.codeforces.follow.database

import android.content.Context
import com.demich.cps.accounts.userinfo.CodeforcesUserInfo
import com.demich.cps.accounts.userinfo.STATUS
import com.demich.cps.platforms.api.codeforces.models.CodeforcesBlogEntry
import com.demich.cps.platforms.api.codeforces.models.CodeforcesLocale
import com.demich.cps.platforms.utils.codeforces.CodeforcesUtils

abstract class CodeforcesFollowList(
    protected val context: Context,
) {
    private val dao: CodeforcesFollowDao =
        CodeforcesFollowDataBase.getInstance(context).followListDao()

    suspend fun remove(handle: String) = dao.remove(handle)

    fun flowOfAllBlogs() = dao.flowOfAllBlogs()

    suspend fun blogs() = dao.getAllBlogs()

    suspend fun getAndReloadBlogEntries(handle: String) =
        dao.getAndReloadBlogEntries(
            handle = handle,
            locale = getLocale(),
            onNewBlogEntry = ::notifyNewBlogEntry
        )

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
    }

    suspend fun updateFailedBlogEntries() {
        dao.getAllBlogs().forEach {
            if (it.blogEntries == null) getAndReloadBlogEntries(handle = it.handle)
        }
    }

    protected abstract suspend fun getLocale(): CodeforcesLocale

    protected abstract fun notifyNewBlogEntry(blogEntry: CodeforcesBlogEntry)
}