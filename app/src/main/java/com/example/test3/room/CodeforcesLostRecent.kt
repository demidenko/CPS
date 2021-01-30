package com.example.test3.room

import android.content.Context
import androidx.room.*
import com.example.test3.utils.CodeforcesUtils

fun getLostBlogsDao(context: Context) = RoomSingleton.getInstance(context).lostBlogsDao()

const val lostBlogsTableName = "cf_lost_blogs"

@Dao
interface LostBlogsDao{
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(blogs: List<LostBlogEntry>)

    @Query("SELECT * FROM $lostBlogsTableName where isSuspect = 0")
    suspend fun getLost(): List<LostBlogEntry>

    @Query("SELECT * FROM $lostBlogsTableName where isSuspect = 1")
    suspend fun getSuspects(): List<LostBlogEntry>
}

@Entity(tableName = lostBlogsTableName)
data class LostBlogEntry(
    @PrimaryKey val id: Int,
    val title: String,
    val authorHandle: String,
    val creationTimeSeconds: Long,
    val authorColorTag: CodeforcesUtils.ColorTag = CodeforcesUtils.ColorTag.BLACK,
    val isSuspect: Boolean,
    val timeStamp: Long
)

class ColorTagConverter {
    @TypeConverter
    fun stringToTag(str: String) = CodeforcesUtils.ColorTag.valueOf(str)
    @TypeConverter
    fun tagToString(tag: CodeforcesUtils.ColorTag) = tag.name
}