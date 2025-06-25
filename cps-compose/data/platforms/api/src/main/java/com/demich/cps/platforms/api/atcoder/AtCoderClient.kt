package com.demich.cps.platforms.api.atcoder

import com.demich.cps.platforms.api.PlatformClient
import com.demich.cps.platforms.api.defaultJson
import com.demich.cps.platforms.api.getText
import com.demich.kotlin_stdlib_boost.ifBetweenFirstFirst
import com.demich.kotlin_stdlib_boost.ifBetweenFirstLast
import io.ktor.client.request.parameter

object AtCoderClient: PlatformClient, AtCoderApi {
    private val json get() = defaultJson

    override suspend fun getUserPage(handle: String): String  {
        return client.getText(urlString = AtCoderUrls.user(handle)) {
            parameter("graph", "rating")
        }
    }

    override suspend fun getMainPage(): String  {
        return client.getText(urlString = AtCoderUrls.main)
    }

    override suspend fun getContestsPage(): String {
        return client.getText(urlString = AtCoderUrls.main + "/contests")
    }

    override suspend fun getRatingChanges(handle: String): List<AtCoderRatingChange> {
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

    override suspend fun getSuggestionsPage(str: String): String {
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
