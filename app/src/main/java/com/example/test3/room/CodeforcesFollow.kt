package com.example.test3.room

import android.content.Context
import androidx.room.*
import com.example.test3.account_manager.CodeforcesUserInfo
import com.example.test3.account_manager.STATUS
import com.example.test3.utils.jsonCPS
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

    @Query("SELECT * FROM $followListTableName")
    suspend fun getAll(): List<CodeforcesUserBlog>

    @Query("SELECT * FROM $followListTableName")
    fun flowOfAll(): Flow<List<CodeforcesUserBlog>>
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