package com.demich.cps.platforms.codeforces.lost

import kotlinx.coroutines.flow.Flow

interface CodeforcesLostBlogEntriesStorage {
    suspend fun addSuspect(suspect: CodeforcesLostSuspect)

    suspend fun addLost(blogEntry: CodeforcesLostBlogEntry)

    fun flowOfLost(): Flow<List<CodeforcesLostBlogEntry>>

    fun flowOfSuspects(): Flow<List<CodeforcesLostSuspect>>

    suspend fun suspectsRemoveInvalid(
        isValid: (CodeforcesLostSuspect) -> Boolean
    ): List<CodeforcesLostSuspect>

    suspend fun removeInvalidLost(
        isInvalid: (CodeforcesLostBlogEntry) -> Boolean
    )
}
