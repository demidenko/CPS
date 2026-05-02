package com.demich.cps.platforms.codeforces.lost

import kotlinx.coroutines.flow.Flow

interface CodeforcesLostBlogEntriesStorage {
    suspend fun insertSuspect(suspect: CodeforcesLostSuspect)

    suspend fun insertLost(blogEntry: CodeforcesLostBlogEntry)

    suspend fun remove(ids: Collection<Int>)

    fun flowOfLost(): Flow<List<CodeforcesLostBlogEntry>>

    fun flowOfSuspects(): Flow<List<CodeforcesLostSuspect>>

    suspend fun lostEntries(): List<CodeforcesLostBlogEntry>

    suspend fun suspects(): List<CodeforcesLostSuspect>
}
