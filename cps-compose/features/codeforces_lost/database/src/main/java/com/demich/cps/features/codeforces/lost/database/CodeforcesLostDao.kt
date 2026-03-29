package com.demich.cps.features.codeforces.lost.database

import android.content.Context
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

val Context.codeforcesLostRepository: CodeforcesLostRepository
    get() = lostDataBase.lostBlogEntriesDao()

interface CodeforcesLostRepository {
    suspend fun insert(blogEntry: CodeforcesLostBlogEntry)

    suspend fun remove(blogEntries: List<CodeforcesLostBlogEntry>)

    fun flowOfLost(): Flow<List<CodeforcesLostBlogEntry>>

    suspend fun getLost(): List<CodeforcesLostBlogEntry>

    suspend fun getSuspects(): List<CodeforcesLostBlogEntry>
}

internal const val cfLostTableName = "BlogEntries"
private const val selectSuspect = "SELECT * FROM $cfLostTableName where isSuspect"

@Dao
internal interface CodeforcesLostDao: CodeforcesLostRepository {
    @Upsert
    override suspend fun insert(blogEntry: CodeforcesLostBlogEntry)

    @Delete
    override suspend fun remove(blogEntries: List<CodeforcesLostBlogEntry>)

    @Query("$selectSuspect = 0")
    override suspend fun getLost(): List<CodeforcesLostBlogEntry>

    @Query("$selectSuspect = 0")
    override fun flowOfLost(): Flow<List<CodeforcesLostBlogEntry>>

    @Query("$selectSuspect = 1")
    override suspend fun getSuspects(): List<CodeforcesLostBlogEntry>
}