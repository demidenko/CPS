package com.demich.cps.utils

import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable


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

object AtCoderAPI {
    private val client = cpsHttpClient { }

    private suspend fun makeWEBCall(block: suspend () -> String): String {
        return kotlin.runCatching {
            withContext(Dispatchers.IO) {
                block()
            }
        }.getOrThrow()
    }

    suspend fun getUserPage(handle: String): String = makeWEBCall {
        client.get(urlString = URLFactory.user(handle)) {
            parameter("graph", "rating")
        }
    }

    object URLFactory {
        const val main = "https://atcoder.jp"
        fun user(handle: String) = "$main/users/$handle"
        fun userContestResult(handle: String, contestId: String) = "$main/users/$handle/history/share/$contestId"
    }
}
