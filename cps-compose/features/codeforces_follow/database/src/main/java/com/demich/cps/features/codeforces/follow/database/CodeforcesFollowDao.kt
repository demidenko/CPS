package com.demich.cps.features.codeforces.follow.database

import androidx.room.*
import com.demich.cps.accounts.userinfo.CodeforcesUserInfo
import com.demich.cps.accounts.userinfo.STATUS
import com.demich.cps.platforms.api.codeforces.CodeforcesApi
import com.demich.cps.platforms.api.codeforces.CodeforcesApiHandleNotFoundException
import com.demich.cps.platforms.api.codeforces.CodeforcesApiNotAllowedReadBlogException
import com.demich.cps.platforms.api.codeforces.models.CodeforcesBlogEntry
import com.demich.cps.platforms.api.codeforces.models.CodeforcesLocale
import com.demich.cps.platforms.utils.codeforces.CodeforcesUtils
import kotlinx.coroutines.flow.Flow

internal const val cfFollowTableName = "FollowList"

@Dao
interface CodeforcesFollowDao {
    @Query("SELECT * FROM $cfFollowTableName")
    suspend fun getAllBlogs(): List<CodeforcesUserBlog>

    @Query("SELECT * FROM $cfFollowTableName")
    fun flowOfAllBlogs(): Flow<List<CodeforcesUserBlog>>

    @Query("SELECT handle FROM $cfFollowTableName")
    suspend fun getHandles(): List<String>

    @Query("SELECT * FROM $cfFollowTableName WHERE handle LIKE :handle")
    suspend fun getUserBlog(handle: String): CodeforcesUserBlog?

    @Insert
    suspend fun insert(blog: CodeforcesUserBlog)

    @Update
    suspend fun update(blog: CodeforcesUserBlog)

    @Query("DELETE FROM $cfFollowTableName WHERE handle LIKE :handle")
    suspend fun remove(handle: String)


    private suspend fun setBlogEntries(handle: String, blogEntries: List<Int>) {
        val userBlog = getUserBlog(handle) ?: return
        if (userBlog.blogEntries != blogEntries) update(userBlog.copy(blogEntries = blogEntries))
    }

    private suspend fun changeHandle(fromHandle: String, toHandle: String) {
        if (fromHandle == toHandle) return
        val fromUserBlog = getUserBlog(fromHandle) ?: return
        getUserBlog(toHandle)?.let { toUserBlog ->
            if (toUserBlog.id != fromUserBlog.id) {
                remove(fromHandle)
                return
            }
        }
        update(fromUserBlog.copy(handle = toHandle))
    }

    private suspend fun setOKUserInfo(handle: String, info: CodeforcesUserInfo) {
        if (info.status != STATUS.OK) return
        if (info.handle != handle) changeHandle(handle, info.handle)
        val userBlog = getUserBlog(info.handle) ?: return
        if (userBlog.userInfo != info) update(userBlog.copy(
            handle = info.handle,
            userInfo = info
        ))
    }

    suspend fun getAndReloadBlogEntries(
        handle: String,
        locale: CodeforcesLocale,
        onNewBlogEntry: (CodeforcesBlogEntry) -> Unit
    ): Result<List<CodeforcesBlogEntry>> {
        return CodeforcesApi.runCatching {
            getUserBlogEntries(handle = handle, locale = locale)
        }.recoverCatching {
            if (it is CodeforcesApiNotAllowedReadBlogException) {
                return@recoverCatching emptyList()
            }
            if (it is CodeforcesApiHandleNotFoundException && it.handle == handle) {
                val userInfo = CodeforcesUtils.getUserInfo(handle = handle, doRedirect = true)
                applyUserInfo(handle, userInfo)
                if (userInfo.status == STATUS.OK) {
                    return@recoverCatching getAndReloadBlogEntries(
                        handle = userInfo.handle,
                        locale = locale,
                        onNewBlogEntry = onNewBlogEntry
                    ).getOrThrow()
                }
            }
            throw it
        }.onSuccess { blogEntries ->
            getUserBlog(handle)?.blogEntries?.toSet()?.let { saved ->
                for (blogEntry in blogEntries) {
                    if (blogEntry.id !in saved) onNewBlogEntry(blogEntry)
                }
            }
            setBlogEntries(handle, blogEntries.map { it.id })
        }
    }

    suspend fun updateUsersInfo() {
        applyUsersInfo(CodeforcesUtils.getUsersInfo(handles = getHandles(), doRedirect = true))
    }

    suspend fun applyUserInfo(handle: String, info: CodeforcesUserInfo) {
        when (info.status) {
            STATUS.OK -> setOKUserInfo(handle, info)
            STATUS.NOT_FOUND -> remove(handle)
            STATUS.FAILED -> Unit
        }
    }

    @Transaction
    suspend fun applyUsersInfo(result: Map<String, CodeforcesUserInfo>) {
        result.forEach { (handle, info) -> applyUserInfo(handle, info) }
    }
}