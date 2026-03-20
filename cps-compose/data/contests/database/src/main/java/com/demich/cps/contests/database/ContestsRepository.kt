package com.demich.cps.contests.database

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

interface ContestsRepository {
    fun flowOfContests(): Flow<List<Contest>>

    suspend fun getContests(platform: Contest.Platform): List<Contest>

    suspend fun getContestsNotFinished(platform: Contest.Platform, currentTime: Instant): List<Contest>

    suspend fun replace(platform: Contest.Platform, contests: List<Contest>)
}

val Context.contestsRepository: ContestsRepository
    get() = contestsDatabase.contestsDao()
