package com.example.test3.room

import androidx.room.*
import com.example.test3.utils.CodeforcesUtils

const val lostBlogsTableName = "cf_lost_blogs"

@Dao
interface LostBlogsDao{
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(blogs: List<LostBlogEntry>)

    @Query("SELECT * FROM $lostBlogsTableName where isSuspect = 0")
    fun getLost(): List<LostBlogEntry>

    @Query("SELECT * FROM $lostBlogsTableName where isSuspect = 1")
    fun getSuspects(): List<LostBlogEntry>
}

@Entity(tableName = lostBlogsTableName)
data class LostBlogEntry(
    @PrimaryKey val id: Int,
    val isSuspect: Boolean,
    val timeStamp: Long,
    val title: String,
    val authorHandle: String,
    val authorColorTag: CodeforcesUtils.ColorTag = CodeforcesUtils.ColorTag.BLACK
)

class ColorTagConverter {
    @TypeConverter
    fun stringToTag(str: String) = CodeforcesUtils.ColorTag.valueOf(str)
    @TypeConverter
    fun tagToString(tag: CodeforcesUtils.ColorTag) = tag.name
}