package com.demich.cps.room

import android.content.Context
import androidx.room.*
import com.demich.cps.utils.codeforces.CodeforcesUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant


val Context.lostBlogEntriesDao get() = RoomSingleton.getInstance(this).lostBlogEntriesDao()

private const val lostBlogEntriesTableName = "cf_lost_blog_entries"

@Dao
interface LostBlogEntriesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(blogEntries: List<CodeforcesLostBlogEntry>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(blogEntry: CodeforcesLostBlogEntry)

    @Update
    suspend fun update(blogEntry: CodeforcesLostBlogEntry)

    @Delete
    suspend fun remove(blogEntries: List<CodeforcesLostBlogEntry>)

    @Delete
    suspend fun remove(blogEntry: CodeforcesLostBlogEntry)

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
    val authorColorTag: CodeforcesUtils.ColorTag = CodeforcesUtils.ColorTag.BLACK,
    val isSuspect: Boolean,
    val timeStamp: Instant
)
