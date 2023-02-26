package com.demich.cps.room

import android.content.Context
import androidx.room.*
import com.demich.cps.utils.codeforces.CodeforcesColorTag
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant


val Context.lostBlogEntriesDao get() = RoomSingleton.getInstance(this).lostBlogEntriesDao()

private const val lostBlogEntriesTableName = "cf_lost_blog_entries"

@Dao
interface LostBlogEntriesDao {
    @Upsert
    suspend fun insert(blogEntry: CodeforcesLostBlogEntry)

    @Delete
    suspend fun remove(blogEntries: List<CodeforcesLostBlogEntry>)

    @Query("SELECT * FROM $lostBlogEntriesTableName where isSuspect = 0")
    suspend fun getLost(): List<CodeforcesLostBlogEntry>

    @Query("SELECT * FROM $lostBlogEntriesTableName where isSuspect = 0")
    fun flowOfLost(): Flow<List<CodeforcesLostBlogEntry>>

    @Query("SELECT * FROM $lostBlogEntriesTableName where isSuspect = 1")
    suspend fun getSuspects(): List<CodeforcesLostBlogEntry>

    @Query("SELECT * FROM $lostBlogEntriesTableName where isSuspect = 1")
    fun flowOfSuspects(): Flow<List<CodeforcesLostBlogEntry>>
}

@Entity(tableName = lostBlogEntriesTableName)
data class CodeforcesLostBlogEntry(
    @PrimaryKey val id: Int,
    val title: String,
    val authorHandle: String,
    val creationTime: Instant,
    val authorColorTag: CodeforcesColorTag = CodeforcesColorTag.BLACK,
    val isSuspect: Boolean,
    val timeStamp: Instant
)
