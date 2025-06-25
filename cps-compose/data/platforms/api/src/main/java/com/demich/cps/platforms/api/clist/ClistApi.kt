package com.demich.cps.platforms.api.clist

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

interface ClistApi {

    suspend fun getContests(
        apiAccess: ApiAccess,
        resourceIds: List<Int>,
        maxStartTime: Instant,
        minEndTime: Instant
    ): List<ClistContest>

    suspend fun getResources(apiAccess: ApiAccess): List<ClistResource>

    @Serializable
    data class ApiAccess(
        val login: String,
        val key: String
    )
}