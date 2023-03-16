package com.demich.cps.room

import android.content.Context
import androidx.room.*
import com.demich.cps.*
import com.demich.cps.accounts.userinfo.CodeforcesUserInfo
import com.demich.cps.accounts.userinfo.STATUS
import com.demich.cps.platforms.api.CodeforcesAPIErrorResponse
import com.demich.cps.platforms.api.CodeforcesApi
import com.demich.cps.platforms.api.CodeforcesBlogEntry
import com.demich.cps.platforms.api.CodeforcesLocale
import com.demich.cps.news.settings.settingsNews
import com.demich.cps.platforms.utils.CodeforcesUtils
import kotlinx.coroutines.flow.Flow

val Context.followListDao: CodeforcesFollowDao
    get() = CodeforcesFollowDao(
        context = this,
        dao = RoomSingleton.getInstance(this).followListDao()
    )

private const val followListTableName = "cf_follow_list"

class CodeforcesFollowDao internal constructor(
    private val context: Context,
    private val dao: FollowListDao
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
        dao.setUserInfo(
            handle = handle,
            info = CodeforcesUtils.getUserInfo(handle = handle, doRedirect = true)
        )
    }

    suspend fun updateUsersInfo() {
        CodeforcesUtils.getUsersInfo(handles = dao.getHandles(), doRedirect = true)
            .forEach { (handle, info) ->
                when (info.status) {
                    STATUS.NOT_FOUND -> dao.remove(handle)
                    STATUS.OK -> dao.setUserInfo(handle, info)
                    STATUS.FAILED -> {}
                }
            }

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
            setSmallIcon(com.demich.cps.R.drawable.ic_new_post)
            setAutoCancel(true)
            setWhen(blogEntry.creationTime)
            attachUrl(url = CodeforcesApi.urls.blogEntry(blogEntry.id), context = context)
        }
}

@Dao
internal interface FollowListDao {

    @Query("SELECT * FROM $followListTableName")
    suspend fun getAllBlogs(): List<CodeforcesUserBlog>

    @Query("SELECT * FROM $followListTableName")
    fun flowOfAllBlogs(): Flow<List<CodeforcesUserBlog>>

    @Query("SELECT handle FROM $followListTableName")
    suspend fun getHandles(): List<String>

    @Query("SELECT * FROM $followListTableName WHERE handle LIKE :handle")
    suspend fun getUserBlog(handle: String): CodeforcesUserBlog?

    @Insert
    suspend fun insert(blog: CodeforcesUserBlog)

    @Update
    suspend fun update(blog: CodeforcesUserBlog)

    @Query("DELETE FROM $followListTableName WHERE handle LIKE :handle")
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

    suspend fun setUserInfo(handle: String, info: CodeforcesUserInfo) {
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
    ): List<CodeforcesBlogEntry>? {
        return CodeforcesApi.runCatching {
            getUserBlogEntries(handle = handle, locale = locale)
        }.recoverCatching {
            if (it is CodeforcesAPIErrorResponse && it.isNotAllowedToReadThatBlog()) {
                return@recoverCatching emptyList()
            }
            if (it is CodeforcesAPIErrorResponse && it.isBlogHandleNotFound(handle)) {
                val userInfo = CodeforcesUtils.getUserInfo(handle = handle, doRedirect = true)
                when (userInfo.status) {
                    STATUS.OK -> {
                        setUserInfo(handle, userInfo)
                        return@recoverCatching getAndReloadBlogEntries(
                            handle = userInfo.handle,
                            locale = locale,
                            onNewBlogEntry = onNewBlogEntry
                        )
                    }
                    STATUS.NOT_FOUND -> {
                        remove(handle)
                    }
                    STATUS.FAILED -> Unit
                }
            }
            throw it
        }.getOrNull()?.also { blogEntries ->
            getUserBlog(handle)?.blogEntries?.toSet()?.let { saved ->
                for (blogEntry in blogEntries) {
                    if (blogEntry.id !in saved) onNewBlogEntry(blogEntry)
                }
            }
            setBlogEntries(handle, blogEntries.map { it.id })
        }
    }
}

@Entity(tableName = followListTableName)
data class CodeforcesUserBlog(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val handle: String,
    val blogEntries: List<Int>?,
    val userInfo: CodeforcesUserInfo
)