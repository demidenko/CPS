package com.demich.cps.features.codeforces.follow.database

import android.content.Context
import com.demich.cps.accounts.userinfo.CodeforcesUserInfo
import com.demich.cps.accounts.userinfo.ProfileResult
import com.demich.cps.accounts.userinfo.toStatusUserInfo
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

    suspend fun addNewUser(result: ProfileResult<CodeforcesUserInfo>) {
        val handle = result.userId
        if (dao.getUserBlog(handle) != null) return
        dao.insert(
            CodeforcesUserBlog(
                handle = handle,
                blogEntries = null,
                userInfo = result.toStatusUserInfo()
            )
        )
        getAndReloadBlogEntries(handle = handle)
    }

    suspend fun addNewUser(handle: String) {
        if (dao.getUserBlog(handle) != null) return
        //TODO: sync?? parallel? (addNewUser loads blog without info)
        addNewUser(ProfileResult.Failed(handle))
        dao.applyProfileResult(
            handle = handle,
            result = CodeforcesUtils.getUserInfo(handle = handle, doRedirect = true)
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