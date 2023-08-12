package com.demich.cps.platforms.api

import io.ktor.client.request.*
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

object AtCoderApi: PlatformApi {
    private val json get() = defaultJson

    suspend fun getUserPage(handle: String): String  {
        return client.getText(urlString = urls.user(handle)) {
            parameter("graph", "rating")
        }
    }

    suspend fun getMainPage(): String  {
        return client.getText(urlString = urls.main)
    }

    suspend fun getContestsPage(): String {
        return client.getText(urlString = urls.main + "/contests")
    }

    suspend fun getRatingChanges(handle: String): List<AtCoderRatingChange> {
        val src = getUserPage(handle)
        val i = src.lastIndexOf("<script>var rating_history=[{")
        if (i == -1) return emptyList()
        val j = src.indexOf("];</script>", i)
        val str = src.substring(src.indexOf('[', i), j+1)
        return json.decodeFromString(str)
    }

    suspend fun getSuggestionsPage(str: String): String {
        return client.getText(urlString = urls.main + "/ranking/all") {
            parameter("f.UserScreenName", str)
            parameter("contestType", "algo")
            parameter("orderBy", "rating")
            parameter("desc", true)
        }
    }

    object urls {
        const val main = "https://atcoder.jp"
        fun user(handle: String) = "$main/users/$handle"
        fun userContestResult(handle: String, contestId: String) = "$main/users/$handle/history/share/$contestId"
        fun contest(id: String) = "$main/contests/$id"
        fun post(id: Int) = "$main/posts/$id"
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
}