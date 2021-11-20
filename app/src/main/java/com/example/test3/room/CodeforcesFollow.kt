package com.example.test3.room

import android.content.Context
import androidx.room.*
import com.example.test3.NewsFragment
import com.example.test3.account_manager.CodeforcesUserInfo
import com.example.test3.account_manager.STATUS
import com.example.test3.utils.*
import com.example.test3.workers.notifyNewBlogEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

fun getFollowDao(context: Context) = RoomSingleton.getInstance(context).followListDao()

const val followListTableName = "cf_follow_list"

@Dao
interface FollowListDao {
    @Query("SELECT * FROM $followListTableName WHERE handle LIKE :handle")
    suspend fun getUserBlog(handle: String): CodeforcesUserBlog?

    @Insert
    suspend fun insert(blog: CodeforcesUserBlog)

    @Update
    suspend fun update(blog: CodeforcesUserBlog)

    @Query("DELETE FROM $followListTableName WHERE handle LIKE :handle")
    suspend fun remove(handle: String)

    @Query("SELECT handle FROM $followListTableName")
    suspend fun getHandles(): List<String>

    @Query("SELECT * FROM $followListTableName")
    fun flowOfAll(): Flow<List<CodeforcesUserBlog>>

    suspend fun getBlogEntries(handle: String) = getUserBlog(handle)?.blogEntries

    suspend fun setBlogEntries(handle: String, blogEntries: List<Int>?) {
        val userBlog = getUserBlog(handle) ?: return
        if(userBlog.blogEntries != blogEntries) update(userBlog.copy(blogEntries = blogEntries))
    }

    suspend fun changeHandle(fromHandle: String, toHandle: String){
        if(fromHandle == toHandle) return
        val fromUserBlog = getUserBlog(fromHandle) ?: return
        getUserBlog(toHandle)?.let { toUserBlog ->
            if(toUserBlog.id != fromUserBlog.id){
                remove(fromHandle)
                return
            }
        }
        update(fromUserBlog.copy(handle = toHandle))
    }

    suspend fun setUserInfo(handle: String, info: CodeforcesUserInfo) {
        if(info.status != STATUS.OK) return
        if(info.handle != handle) changeHandle(handle, info.handle)
        val userBlog = getUserBlog(info.handle) ?: return
        if(userBlog.userInfo != info) update(userBlog.copy(
            handle = info.handle,
            userInfo = info
        ))
    }

    suspend fun loadBlogEntries(handle: String, context: Context): List<CodeforcesBlogEntry> {
        val locale = NewsFragment.getCodeforcesContentLanguage(context)
        val response = CodeforcesAPI.getUserBlogEntries(handle, locale) ?: return emptyList()
        val blogEntries =
            if(response.status == CodeforcesAPIStatus.FAILED) {
                when {
                    response.isBlogHandleNotFound(handle) -> {
                        val (realHandle, status) = CodeforcesUtils.getRealHandle(handle)
                        return when(status){
                            STATUS.OK -> {
                                changeHandle(handle, realHandle)
                                loadBlogEntries(realHandle, context)
                            }
                            STATUS.NOT_FOUND -> {
                                remove(handle)
                                emptyList()
                            }
                            STATUS.FAILED -> emptyList()
                        }
                    }
                    response.isNotAllowedToReadThatBlog() -> emptyList()
                    else -> return emptyList()
                }
            } else response.result ?: return emptyList()
        getBlogEntries(handle)?.toSet()?.let { saved ->
            for(blogEntry in  blogEntries) {
                if(blogEntry.id !in saved) notifyNewBlogEntry(blogEntry, context)
            }
        }
        setBlogEntries(handle, blogEntries.map { it.id })
        return blogEntries
    }

    suspend fun updateUsersInfo(context: Context) {
        CodeforcesUtils.getUsersInfo(getHandles(), true)
            .forEach { (handle, info) ->
                when (info.status) {
                    STATUS.NOT_FOUND -> remove(handle)
                    STATUS.OK -> setUserInfo(handle, info)
                    STATUS.FAILED -> throw IllegalArgumentException("cant be FAILED here")
                }
            }
        getHandles().forEach { handle ->
            if(getBlogEntries(handle) == null) loadBlogEntries(handle, context)
        }
    }
}

@Entity(tableName = followListTableName)
data class CodeforcesUserBlog(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val handle: String,

    @ColumnInfo(name = "blogs")
    val blogEntries: List<Int>?,

    val userInfo: CodeforcesUserInfo
)

class IntsListConverter {
    @TypeConverter
    fun intsToString(ints: List<Int>?): String? {
        if(ints == null) return null
        return buildString {
            ints.forEach { num ->
                var x = num
                repeat(4){
                    append((x%256).toChar())
                    x/=256
                }
            }
        }
    }

    @TypeConverter
    fun decodeToInts(s: String?): List<Int>? {
        if(s == null) return null
        return (s.indices step 4).map { i ->
            ((s[i+3].code*256 + s[i+2].code)*256 + s[i+1].code)*256 + s[i].code
        }
    }
}

class CodeforcesUserInfoConverter {
    @TypeConverter
    fun userInfoToString(info: CodeforcesUserInfo): String {
        return jsonCPS.encodeToString(info)
    }

    @TypeConverter
    fun stringToUserInfo(str: String): CodeforcesUserInfo {
        return try {
            jsonCPS.decodeFromString(str)
        } catch (e: SerializationException) {
            CodeforcesUserInfo(
                status = STATUS.FAILED,
                handle = ""
            )
        }
    }
}