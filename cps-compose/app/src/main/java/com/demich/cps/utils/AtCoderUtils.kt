package com.demich.cps.utils

import com.demich.cps.accounts.managers.RatingChange
import io.ktor.client.request.*
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

object AtCoderApi {
    private val client = cpsHttpClient(json = false) { }

    suspend fun getUserPage(handle: String): String  {
        return client.getText(urlString = urls.user(handle)) {
            parameter("graph", "rating")
        }
    }

    suspend fun getContestsPage(): String {
        return client.getText(urlString = urls.main + "/contests")
    }

    suspend fun getRatingChanges(handle: String): List<AtCoderRatingChange> {
        val s = getUserPage(handle)
        val i = s.lastIndexOf("<script>var rating_history=[{")
        if (i == -1) return emptyList()
        val j = s.indexOf("];</script>", i)
        val str = s.substring(s.indexOf('[', i), j+1)
        return jsonCPS.decodeFromString(str)
    }

    object urls {
        const val main = "https://atcoder.jp"
        fun user(handle: String) = "$main/users/$handle"
        fun userContestResult(handle: String, contestId: String) = "$main/users/$handle/history/share/$contestId"
    }
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

    fun toRatingChange(handle: String) =
        RatingChange(
            rating = NewRating,
            oldRating = OldRating,
            rank = Place,
            date = EndTime,
            title = ContestName,
            url = AtCoderApi.urls.userContestResult(handle, getContestId())
        )
}
