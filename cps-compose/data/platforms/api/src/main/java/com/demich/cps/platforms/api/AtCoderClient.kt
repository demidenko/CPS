package com.demich.cps.platforms.api

import com.demich.kotlin_stdlib_boost.ifBetweenFirstFirst
import com.demich.kotlin_stdlib_boost.ifBetweenFirstLast
import io.ktor.client.request.parameter
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

object AtCoderClient: PlatformClient {
    private val json get() = defaultJson

    suspend fun getUserPage(handle: String): String  {
        return client.getText(urlString = AtCoderUrls.user(handle)) {
            parameter("graph", "rating")
        }
    }

    suspend fun getMainPage(): String  {
        return client.getText(urlString = AtCoderUrls.main)
    }

    suspend fun getContestsPage(): String {
        return client.getText(urlString = AtCoderUrls.main + "/contests")
    }

    suspend fun getRatingChanges(handle: String): List<AtCoderRatingChange> {
        ifBetweenFirstFirst(
            str = getUserPage(handle),
            from = "<script>var rating_history",
            to = ";</script>"
        ) { str ->
            ifBetweenFirstLast(str, from = "[", to = "]", include = true) {
                return json.decodeFromString(it)
            }
        }
        return emptyList()
    }

    suspend fun getSuggestionsPage(str: String): String {
        return client.getText(urlString = AtCoderUrls.main + "/ranking/all") {
            parameter("f.UserScreenName", str)
            parameter("contestType", "algo")
            parameter("orderBy", "rating")
            parameter("desc", true)
        }
    }
}

object AtCoderUrls {
    const val main = "https://atcoder.jp"
    fun user(handle: String) = "$main/users/$handle"
    fun userContestResult(handle: String, contestId: String) = "$main/users/$handle/history/share/$contestId"
    fun contest(id: String) = "$main/contests/$id"
    fun post(id: Int) = "$main/posts/$id"
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