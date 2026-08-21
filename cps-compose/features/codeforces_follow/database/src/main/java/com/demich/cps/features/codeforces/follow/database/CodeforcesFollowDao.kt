package com.demich.cps.features.codeforces.follow.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
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

    @Query("SELECT * FROM $cfFollowTableName WHERE id == :id")
    @RewriteQueriesToDropUnusedColumns
    abstract suspend fun getShortEntity(id: Long): CodeforcesUserBlogEntityShort?

    @Query("SELECT * FROM $cfFollowTableName WHERE id == :id")
    @RewriteQueriesToDropUnusedColumns
    abstract fun flowOfShortEntity(id: Long): Flow<CodeforcesUserBlogEntityShort?>

    @Query("SELECT * FROM $cfFollowTableName")
    @RewriteQueriesToDropUnusedColumns
    abstract suspend fun getShortBlogs(): List<CodeforcesUserBlogEntityShort>

    @Query("SELECT * FROM $cfFollowTableName ORDER BY id DESC")
    @RewriteQueriesToDropUnusedColumns
    abstract fun flowOfShortBlogs(): Flow<List<CodeforcesUserBlogEntityShort>>

    @Query("SELECT * FROM $cfFollowTableName WHERE handle LIKE :handle")
    protected abstract suspend fun getEntity(handle: String): CodeforcesUserBlogEntity?

    @Query("SELECT * FROM $cfFollowTableName WHERE handle LIKE :handle")
    @RewriteQueriesToDropUnusedColumns
    protected abstract suspend fun getUserInfoFields(handle: String): UserInfoFields?

    @Query("SELECT 1 FROM $cfFollowTableName WHERE handle LIKE :handle")
    abstract suspend fun hasUser(handle: String): Boolean

    @Query("SELECT handle FROM $cfFollowTableName")
    abstract suspend fun usersHandles(): List<String>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun insert(entity: CodeforcesUserBlogEntity): Long

    @Update
    protected abstract suspend fun update(entity: CodeforcesUserBlogEntity)

    @Update(entity = CodeforcesUserBlogEntity::class)
    protected abstract suspend fun update(userInfoFields: UserInfoFields)

    @Query("DELETE FROM $cfFollowTableName WHERE handle LIKE :handle")
    protected abstract suspend fun remove(handle: String)

    @Query("DELETE FROM $cfFollowTableName WHERE id == :blogId")
    abstract suspend fun remove(blogId: Long)

    private suspend fun changeHandle(fromHandle: String, toHandle: String) {
        if (fromHandle == toHandle) return
        val fromUserBlog = getEntity(fromHandle) ?: return
        getEntity(toHandle)?.let { toUserBlog ->
            if (toUserBlog.id != fromUserBlog.id) {
                remove(blogId = fromUserBlog.id)
                return
            }
        }
        update(fromUserBlog.copy(handle = toHandle))
    }

    private suspend fun setUserInfo(handle: String, userInfo: CodeforcesUserInfo) {
        if (userInfo.handle != handle) changeHandle(handle, userInfo.handle)
        val handle = userInfo.handle
        val entity = getUserInfoFields(handle) ?: return
        if (entity.userInfo != userInfo) {
            update(userInfoFields = entity.copy(handle = handle, userInfo = userInfo))
        }
    }

    @Transaction
    open suspend fun updateUserProfiles(results: Map<String, ProfileResult<CodeforcesUserInfo>>) {
        results.forEach { (handle, result) ->
            when (result) {
                is ProfileResult.Success -> setUserInfo(handle, result.userInfo)
                is ProfileResult.NotFound -> remove(handle = handle)
                is ProfileResult.Failed -> { }
            }
        }
    }

    @IgnorableReturnValue
    suspend fun updateBlogEntries(
        handle: String,
        blogEntries: List<CodeforcesBlogEntry>,
        onNewBlogEntries: (List<CodeforcesBlogEntry>) -> Unit
    ): CodeforcesUserBlogEntity? {
        val blogEntity = getEntity(handle) ?: return null
        val newBlogEntity = blogEntity.updateBlogInfo(blogEntries, onNewBlogEntries)
        if (newBlogEntity !== blogEntity) update(entity = newBlogEntity)
        return newBlogEntity
    }

    @IgnorableReturnValue
    suspend fun createUserWithoutBlog(profileResult: ProfileResult<CodeforcesUserInfo>): Boolean {
        if (profileResult is ProfileResult.NotFound) return false

        val rowId = insert(
            CodeforcesUserBlogEntity(
                handle = profileResult.handle,
                userInfo = profileResult.userInfoOrNull(),
                blogInfo = null
            )
        )

        return rowId != -1L
    }
}

internal suspend fun CodeforcesFollowDao.updateUserProfile(
    handle: String,
    result: ProfileResult<CodeforcesUserInfo>
) = updateUserProfiles(results = mapOf(handle to result))

private fun CodeforcesUserBlogEntity.updateBlogInfo(
    blogEntries: List<CodeforcesBlogEntry>,
    onNewBlogEntries: (List<CodeforcesBlogEntry>) -> Unit
): CodeforcesUserBlogEntity {
    if (blogInfo == null) {
        return copy(blogInfo = BlogInfo(blogSize = blogEntries.size, savedIds = blogEntries.map { it.id }.toSet()))
    }

    val newBlogInfo = blogInfo.merge(blogEntries, onNewBlogEntries)
    if (newBlogInfo === blogInfo) return this
    return copy(blogInfo = newBlogInfo)
}

private fun BlogInfo.merge(
    blogEntries: List<CodeforcesBlogEntry>,
    onNewBlogEntries: (List<CodeforcesBlogEntry>) -> Unit
): BlogInfo {
    // no changes
    if (blogSize == blogEntries.size && blogEntries.all { it.id in savedIds }) return this

    val newToSave = blogEntries.filter { it.id !in savedIds }
    onNewBlogEntries(newToSave)

    val newIds = buildSet {
        addAll(savedIds)
        newToSave.forEach { add(it.id) }
    }

    return BlogInfo(blogSize = blogEntries.size, savedIds = newIds)
}

internal data class UserInfoFields(
    val id: Long,
    val handle: String,
    val userInfo: CodeforcesUserInfo?
)