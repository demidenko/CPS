package com.example.test3.room

import android.content.Context
import androidx.room.*

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
}

@Entity(tableName = followListTableName)
data class CodeforcesUserBlog(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val handle: String,

    @ColumnInfo(name = "blogs")
    val blogEntries: List<Int>?
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