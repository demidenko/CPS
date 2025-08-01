package com.demich.cps.features.codeforces.follow.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.demich.cps.accounts.userinfo.CodeforcesUserInfo
import com.demich.cps.accounts.userinfo.ProfileResult
import com.demich.cps.platforms.api.codeforces.models.CodeforcesBlogEntry
import kotlinx.coroutines.flow.Flow

internal const val cfFollowTableName = "FollowList"

@Dao
internal interface CodeforcesFollowDao {
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
    suspend fun setUserInfo(handle: String, info: CodeforcesUserInfo) {
        if (info.handle != handle) changeHandle(handle, info.handle)
        val userBlog = getUserBlog(info.handle) ?: return
        if (userBlog.userInfo != info) update(userBlog.copy(
            handle = info.handle,
            userInfo = info
        ))
    }

    suspend fun addBlogEntries(
        handle: String,
        blogEntries: List<CodeforcesBlogEntry>,
        onNewBlogEntry: (CodeforcesBlogEntry) -> Unit
    ) {
        val userBlog = getUserBlog(handle) ?: return
        val currentIds = userBlog.blogEntries?.toSet()
        val newIds = mutableListOf<Int>()
        blogEntries.forEach {
            if (currentIds == null || it.id !in currentIds) {
                newIds.add(it.id)
                if (currentIds != null) onNewBlogEntry(it)
            }
        }
        if (currentIds == null) {
            update(userBlog.copy(blogEntries = newIds))
        } else {
            if (newIds.isNotEmpty()) update(userBlog.copy(blogEntries = newIds + currentIds))
        }
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
