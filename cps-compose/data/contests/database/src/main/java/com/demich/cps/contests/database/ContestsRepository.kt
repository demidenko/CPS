package com.demich.cps.contests.database

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

interface ContestsRepository {
    fun flowOfContests(): Flow<List<Contest>>

    suspend fun getContests(platform: ContestPlatform): List<Contest>

    suspend fun getContestsNotFinished(platform: ContestPlatform, currentTime: Instant): List<Contest>

    suspend fun setContests(platform: ContestPlatform, contests: List<Contest>)
}

val Context.contestsRepository: ContestsRepository
    get() = contestsDatabase.contestsDao()
