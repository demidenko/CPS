package com.demich.cps.utils

import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.http.*
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
    private val client = cpsHttpClient {
        HttpResponseValidator {
            handleResponseException { exception ->
                if (exception !is ResponseException) return@handleResponseException
                val exceptionResponse = exception.response
                if (exceptionResponse.status == HttpStatusCode.NotFound) {
                    throw AtCoderPageNotFoundException()
                }
                throw exception
            }
        }
    }

    class AtCoderPageNotFoundException : Throwable("AtCoderAPI: page not found")

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

    suspend fun getRatingChanges(handle: String): List<AtCoderRatingChange>? {
        return null //TODO
    }

    object URLFactory {
        const val main = "https://atcoder.jp"
        fun user(handle: String) = "$main/users/$handle"
        fun userContestResult(handle: String, contestId: String) = "$main/users/$handle/history/share/$contestId"
    }
}
