package com.demich.cps.platforms.api.atcoder

import com.demich.cps.platforms.api.InstantAsSecondsSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

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
    val StandingsUrl: String
) {
    fun getContestId(): String {
        val s = StandingsUrl.removePrefix("/contests/")
        return s.substring(0, s.indexOf('/'))
    }
}