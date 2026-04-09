package com.demich.cps.features.codeforces.follow.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.demich.cps.platforms.api.codeforces.models.CodeforcesBlogEntry
import com.demich.cps.profiles.userinfo.CodeforcesUserInfo
import com.demich.cps.profiles.userinfo.ProfileResult
import kotlinx.coroutines.flow.Flow

internal const val cfFollowTableName = "FollowList"

@Dao
internal interface CodeforcesFollowDao {

    @Query("SELECT * FROM $cfFollowTableName")
    suspend fun getShortBlogs(): List<CodeforcesUserBlogEntityShort>

    @Query("SELECT * FROM $cfFollowTableName ORDER BY id DESC")
    fun flowOfShortBlogs(): Flow<List<CodeforcesUserBlogEntityShort>>

    @Query("SELECT * FROM $cfFollowTableName WHERE handle LIKE :handle")
    suspend fun getUserBlog(handle: String): CodeforcesUserBlogEntity?

    @Query("SELECT 1 FROM $cfFollowTableName WHERE handle LIKE :handle")
    suspend fun hasUser(handle: String): Boolean

    @Query("SELECT handle FROM $cfFollowTableName")
    suspend fun getHandles(): List<String>

    @Insert
    suspend fun insert(blog: CodeforcesUserBlogEntity)

    @Update
    suspend fun update(blog: CodeforcesUserBlogEntity)

    @Query("DELETE FROM $cfFollowTableName WHERE handle LIKE :handle")
    suspend fun remove(handle: String)

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

    @Transaction
    suspend fun setUserInfo(handle: String, userInfo: CodeforcesUserInfo) {
        if (userInfo.handle != handle) changeHandle(handle, userInfo.handle)
        val handle = userInfo.handle
        val userBlog = getUserBlog(handle) ?: return
        if (userBlog.userInfo != userInfo) {
            update(userBlog.copy(handle = handle, userInfo = userInfo))
        }
    }

    suspend fun updateBlogEntries(
        handle: String,
        blogEntries: List<CodeforcesBlogEntry>,
        onNewBlogEntry: (CodeforcesBlogEntry) -> Unit
    ) {
        val userBlog = getUserBlog(handle) ?: return
        val newUserBlog = merge(userBlog, blogEntries, onNewBlogEntry) ?: return

        update(newUserBlog)
    }

    suspend fun applyProfileResult(handle: String, result: ProfileResult<CodeforcesUserInfo>) {
        when (result) {
            is ProfileResult.Success -> setUserInfo(handle, result.userInfo)
            is ProfileResult.NotFound -> remove(handle)
            is ProfileResult.Failed -> { }
        }
    }

    @Transaction
    suspend fun applyProfilesResults(results: Map<String, ProfileResult<CodeforcesUserInfo>>) {
        results.forEach { applyProfileResult(handle = it.key, result = it.value) }
    }
}

private fun merge(
    userBlog: CodeforcesUserBlogEntity,
    blogEntries: List<CodeforcesBlogEntry>,
    onNewBlogEntry: (CodeforcesBlogEntry) -> Unit
): CodeforcesUserBlogEntity? {

    userBlog.blogInfo?.run {
        // no changes
        if (blogSize == blogEntries.size && blogEntries.all { it.id in savedIds }) return null
    }

    val savedIds = userBlog.blogInfo?.savedIds

    val newIds = mutableSetOf<Int>()
    blogEntries.forEach {
        if (savedIds == null || it.id !in savedIds) {
            newIds.add(it.id)
            if (savedIds != null) onNewBlogEntry(it)
        }
    }

    if (savedIds != null) {
        newIds += savedIds
    }

    val newBlogInfo = BlogInfo(savedIds = newIds, blogSize = blogEntries.size)
    return userBlog.copy(blogInfo = newBlogInfo)
}