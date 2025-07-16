package com.demich.cps.features.codeforces.follow.database

import android.content.Context
import com.demich.cps.accounts.userinfo.CodeforcesUserInfo
import com.demich.cps.accounts.userinfo.ProfileResult
import com.demich.cps.accounts.userinfo.handle
import com.demich.cps.accounts.userinfo.userInfoOrNull
import com.demich.cps.platforms.api.codeforces.CodeforcesApi
import com.demich.cps.platforms.api.codeforces.CodeforcesApiHandleNotFoundException
import com.demich.cps.platforms.api.codeforces.CodeforcesApiNotAllowedReadBlogException
import com.demich.cps.platforms.api.codeforces.models.CodeforcesBlogEntry
import com.demich.cps.platforms.api.codeforces.models.CodeforcesLocale
import com.demich.cps.platforms.utils.codeforces.getProfile
import com.demich.cps.platforms.utils.codeforces.getProfiles

abstract class CodeforcesFollowRepository(
    private val api: CodeforcesApi,
    context: Context
) {
    private val dao: CodeforcesFollowDao =
        context.followDataBase.followListDao()

    suspend fun remove(handle: String) = dao.remove(handle)

    fun flowOfAllBlogs() = dao.flowOfAllBlogs()

    suspend fun blogs() = dao.getAllBlogs()

    suspend fun getAndReloadBlogEntries(handle: String) =
        getAndReloadBlogEntries(handle = handle, locale = getLocale())

    //TODO: refactor this
    private suspend fun getAndReloadBlogEntries(
        handle: String,
        locale: CodeforcesLocale
    ): Result<List<CodeforcesBlogEntry>> {
        return api.runCatching {
            getUserBlogEntries(handle = handle, locale = locale)
        }.recoverCatching {
            if (it is CodeforcesApiNotAllowedReadBlogException) {
                return@recoverCatching emptyList()
            }
            if (it is CodeforcesApiHandleNotFoundException && it.handle == handle) {
                val profileResult = api.getProfile(handle = handle, recoverHandle = true)
                dao.applyProfileResult(handle, profileResult)
                if (profileResult is ProfileResult.Success) {
                    return@recoverCatching getAndReloadBlogEntries(
                        handle = profileResult.userInfo.handle,
                        locale = locale
                    ).getOrThrow()
                }
            }
            throw it
        }.onSuccess { blogEntries ->
            dao.addBlogEntries(handle, blogEntries, ::notifyNewBlogEntry)
        }
    }

    suspend fun addNewUser(result: ProfileResult<CodeforcesUserInfo>) {
        if (result is ProfileResult.NotFound) return

        val handle = result.handle
        if (dao.getUserBlog(handle) != null) return
        dao.insert(
            CodeforcesUserBlog(
                handle = handle,
                blogEntries = null,
                userInfo = result.userInfoOrNull()
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
            result = api.getProfile(handle = handle, recoverHandle = true)
        )
    }

    suspend fun updateUsers() {
        val profiles = api.getProfiles(handles = dao.getHandles(), recoverHandle = true)
        dao.applyProfilesResults(profiles)
    }

    suspend fun updateFailedBlogEntries() {
        dao.getAllBlogs().forEach {
            if (it.blogEntries == null) getAndReloadBlogEntries(handle = it.handle)
        }
    }

    protected abstract suspend fun getLocale(): CodeforcesLocale

    protected abstract fun notifyNewBlogEntry(blogEntry: CodeforcesBlogEntry)
}