package com.demich.cps.platforms.api.clist

import kotlin.time.Instant

interface ClistApi {

    suspend fun getContests(
        resourceIds: Collection<Int>,
        maxStartTime: Instant,
        minEndTime: Instant
    ): List<ClistContest>

    suspend fun getResources(): List<ClistResource>

    data class ApiAccess(
        val login: String,
        val key: String
    )
}