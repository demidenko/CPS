package com.demich.cps.platforms.api.atcoder

import com.demich.cps.platforms.api.InstantAsSecondsSerializer
import kotlinx.serialization.Serializable
import kotlin.time.Instant

interface AtCoderApi {
    suspend fun getUserPage(handle: String): String

    suspend fun getMainPage(): String

    suspend fun getContestsPage(): String

    suspend fun getSuggestionsPage(str: String): String

    suspend fun getRatingChanges(handle: String): List<AtCoderRatingChange>
}

@Serializable
data class AtCoderRatingChange(
    val NewRating: Int,
    val OldRating: Int,
    val Place: Int,
    @Serializable(with = InstantAsSecondsSerializer::class)
    val EndTime: Instant,
    val ContestName: String,
    val StandingsUrl: String //relative
)