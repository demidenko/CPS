package com.demich.cps.features.codeforces.lost.database

import android.content.Context
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

val Context.lostBlogEntriesDao: CodeforcesLostDao
    get() = CodeforcesLostDataBase.getInstance(this).lostBlogEntriesDao()


internal const val cfLostTableName = "BlogEntries"
private const val selectSuspect = "SELECT * FROM $cfLostTableName where isSuspect"

@Dao
interface CodeforcesLostDao {
    @Upsert
    suspend fun insert(blogEntries: List<CodeforcesLostBlogEntry>)

    @Delete
    suspend fun remove(blogEntries: List<CodeforcesLostBlogEntry>)

    @Query("$selectSuspect = 0")
    suspend fun getLost(): List<CodeforcesLostBlogEntry>

    @Query("$selectSuspect = 0")
    fun flowOfLost(): Flow<List<CodeforcesLostBlogEntry>>

    @Query("$selectSuspect = 1")
    suspend fun getSuspects(): List<CodeforcesLostBlogEntry>

    @Query("$selectSuspect = 1")
    fun flowOfSuspects(): Flow<List<CodeforcesLostBlogEntry>>
}