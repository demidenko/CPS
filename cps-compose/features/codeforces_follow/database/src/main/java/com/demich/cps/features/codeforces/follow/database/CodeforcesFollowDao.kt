package com.demich.cps.features.codeforces.follow.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.demich.cps.platforms.api.codeforces.models.CodeforcesBlogEntry
import com.demich.cps.profiles.userinfo.CodeforcesUserInfo
import com.demich.cps.profiles.userinfo.ProfileResult
import com.demich.cps.profiles.userinfo.handle
import com.demich.cps.profiles.userinfo.userInfoOrNull
import kotlinx.coroutines.flow.Flow

internal const val cfFollowTableName = "FollowList"

@Dao
internal abstract class CodeforcesFollowDao {

    @Query("SELECT * FROM $cfFollowTableName")
    abstract suspend fun getShortBlogs(): List<CodeforcesUserBlogEntityShort>

    @Query("SELECT * FROM $cfFollowTableName ORDER BY id DESC")
    abstract fun flowOfShortBlogs(): Flow<List<CodeforcesUserBlogEntityShort>>

    @Query("SELECT * FROM $cfFollowTableName WHERE handle LIKE :handle")
    protected abstract suspend fun getEntity(handle: String): CodeforcesUserBlogEntity?

    @Query("SELECT * FROM $cfFollowTableName WHERE handle LIKE :handle")
    protected abstract suspend fun getUserInfoFields(handle: String): UserInfoFields?

    @Query("SELECT 1 FROM $cfFollowTableName WHERE handle LIKE :handle")
    abstract suspend fun hasUser(handle: String): Boolean

    @Query("SELECT handle FROM $cfFollowTableName")
    abstract suspend fun getHandles(): List<String>

    @Insert
    protected abstract suspend fun insert(entity: CodeforcesUserBlogEntity)

    @Update
    protected abstract suspend fun update(entity: CodeforcesUserBlogEntity)

    @Update(entity = CodeforcesUserBlogEntity::class)
    protected abstract suspend fun update(userInfoFields: UserInfoFields)

    @Query("DELETE FROM $cfFollowTableName WHERE handle LIKE :handle")
    abstract suspend fun remove(handle: String)

    private suspend fun changeHandle(fromHandle: String, toHandle: String) {
        if (fromHandle == toHandle) return
        val fromUserBlog = getEntity(fromHandle) ?: return
        getEntity(toHandle)?.let { toUserBlog ->
            if (toUserBlog.id != fromUserBlog.id) {
                remove(fromHandle)
                return
            }
        }
        update(fromUserBlog.copy(handle = toHandle))
    }

    @Transaction
    protected open suspend fun setUserInfo(handle: String, userInfo: CodeforcesUserInfo) {
        if (userInfo.handle != handle) changeHandle(handle, userInfo.handle)
        val handle = userInfo.handle
        val entity = getUserInfoFields(handle) ?: return
        if (entity.userInfo != userInfo) {
            update(userInfoFields = entity.copy(handle = handle, userInfo = userInfo))
        }
    }

    suspend fun updateBlogEntries(
        handle: String,
        blogEntries: List<CodeforcesBlogEntry>,
        onNewBlogEntry: (CodeforcesBlogEntry) -> Unit
    ): CodeforcesUserBlogEntity? {
        val blogEntity = getEntity(handle) ?: return null
        val newBlogEntity = blogEntity.updateBlogInfo(blogEntries, onNewBlogEntry) ?: return blogEntity
        update(newBlogEntity)
        return newBlogEntity
    }

    suspend fun applyProfileResult(handle: String, result: ProfileResult<CodeforcesUserInfo>) {
        when (result) {
            is ProfileResult.Success -> setUserInfo(handle, result.userInfo)
            is ProfileResult.NotFound -> remove(handle)
            is ProfileResult.Failed -> { }
        }
    }

    @Transaction
    open suspend fun applyProfilesResults(results: Map<String, ProfileResult<CodeforcesUserInfo>>) {
        results.forEach { applyProfileResult(handle = it.key, result = it.value) }
    }

    suspend fun insertWithoutBlog(profileResult: ProfileResult<CodeforcesUserInfo>) {
        insert(
            CodeforcesUserBlogEntity(
                handle = profileResult.handle,
                userInfo = profileResult.userInfoOrNull(),
                blogInfo = null
            )
        )
    }
}

private fun CodeforcesUserBlogEntity.updateBlogInfo(
    blogEntries: List<CodeforcesBlogEntry>,
    onNewBlogEntry: (CodeforcesBlogEntry) -> Unit
): CodeforcesUserBlogEntity? {
    blogInfo?.run {
        // no changes
        if (blogSize == blogEntries.size && blogEntries.all { it.id in savedIds }) return null
    }

    val newToSave = newEntriesToSave(blogEntries)
    if (blogInfo != null) {
        newToSave.forEach(onNewBlogEntry)
    }

    val newIds = buildSet {
        if (blogInfo != null) addAll(blogInfo.savedIds)
        newToSave.forEach { add(it.id) }
    }

    return copy(blogInfo = BlogInfo(savedIds = newIds, blogSize = blogEntries.size))
}

private fun CodeforcesUserBlogEntity.newEntriesToSave(
    blogEntries: List<CodeforcesBlogEntry>
): List<CodeforcesBlogEntry> {
    if (blogInfo == null) return blogEntries
    val savedIds = blogInfo.savedIds
    return blogEntries.filter { it.id !in savedIds }
}

internal data class UserInfoFields(
    val id: Long,
    val handle: String,
    val userInfo: CodeforcesUserInfo?
)