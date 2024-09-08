package com.demich.cps.platforms.api.codeforces

import com.demich.cps.platforms.api.PlatformApi
import com.demich.cps.platforms.api.codeforces.models.*
import com.demich.cps.platforms.api.cpsHttpClient
import com.demich.cps.platforms.api.decodeAES
import com.demich.cps.platforms.api.defaultJson
import com.demich.cps.platforms.api.getAs
import com.demich.cps.platforms.api.getText
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

object CodeforcesApi: PlatformApi {
    private val json get() = defaultJson

    override val client = cpsHttpClient(json = json) {
        defaultRequest {
            url(urls.main)
        }
        HttpResponseValidator {
            /*TODO: DoubleReceiveException after bodyAsText()
            validateResponse {
                if (it.status.value == 200) {
                    val text = it.bodyAsText()
                    if (isTemporarilyUnavailable(text)) throw CodeforcesTemporarilyUnavailableException()
                }
            }*/
            handleResponseExceptionWithRequest { exception, _ ->
                if (exception !is ResponseException) return@handleResponseExceptionWithRequest
                val response = exception.response
                json.runCatching { decodeFromString<CodeforcesAPIErrorResponse>(response.bodyAsText()) }
                    .onSuccess { throw it.mapOrThis() }
                    .onFailure { throw exception }
            }
        }
    }

    class CodeforcesTemporarilyUnavailableException: CodeforcesApiException("Codeforces Temporarily Unavailable")

    private val callLimitExceededWaitTime: Duration get() = 500.milliseconds
    private val redirectWaitTime: Duration get() = 300.milliseconds
    private fun isCallLimitExceeded(e: Throwable): Boolean {
        if (e is CodeforcesApiCallLimitExceededException) return true
        if (e is ResponseException && e.response.status == HttpStatusCode.ServiceUnavailable) return true
        return false
    }

    private suspend fun<T> responseWithRetry(
        remainingRetries: Int,
        get: suspend () -> CodeforcesAPIResponse<T>
    ): CodeforcesAPIResponse<T> {
        return kotlin.runCatching { get() }.getOrElse { exception ->
            if (isCallLimitExceeded(exception) && remainingRetries > 0) {
                delay(callLimitExceededWaitTime)
                responseWithRetry(remainingRetries - 1, get)
            } else {
                throw exception
            }
        }
    }

    private suspend inline fun<reified T> getCodeforcesApi(
        path: String,
        crossinline block: HttpRequestBuilder.() -> Unit = {}
    ): T {
        return responseWithRetry(remainingRetries = 9) {
            client.getAs<CodeforcesAPIResponse<T>>(urlString = "/api/$path", block = block)
        }.result
    }

    private val RCPC = object {
        private var rcpcToken: String = ""

        private var last_c = ""
        private fun calculateToken(source: String) {
            val i = source.indexOf("c=toNumbers(")
            val c = source.substring(source.indexOf("(\"",i)+2, source.indexOf("\")",i))
            if (c == last_c) return
            rcpcToken = decodeAES(c)
            last_c = c
            println("$c: $rcpcToken")
        }

        private fun String.isRCPCCase() =
            startsWith("<html><body>Redirecting... Please, wait.")

        suspend inline fun getPage(get: (String) -> String): String {
            val s = get(rcpcToken)
            return if (s.isRCPCCase()) {
                calculateToken(s)
                delay(redirectWaitTime)
                get(rcpcToken)
            } else s
        }
    }

    private suspend inline fun getCodeforcesWeb(
        path: String,
        block: HttpRequestBuilder.() -> Unit = {}
    ): String {
        return RCPC.getPage { rcpcToken ->
            client.getText(path) {
                if (rcpcToken.isNotEmpty()) header("Cookie", "RCPC=$rcpcToken")
                block()
            }.also {
                //TODO: check this for api requests too (in validateResponse)
                if (isTemporarilyUnavailable(it)) throw CodeforcesTemporarilyUnavailableException()
            }
        }
    }


    suspend fun getUsers(
        handles: Collection<String>,
        checkHistoricHandles: Boolean = false
    ): List<CodeforcesUser> {
        if (handles.isEmpty()) return emptyList()
        return getCodeforcesApi(path = "user.info") {
            parameter("handles", handles.joinToString(separator = ";"))
            parameter("checkHistoricHandles", checkHistoricHandles)
        }
    }

    suspend fun getUser(handle: String, checkHistoricHandles: Boolean = false): CodeforcesUser {
        return getUsers(listOf(handle), checkHistoricHandles).first()
    }

    suspend fun getUserRatingChanges(handle: String): List<CodeforcesRatingChange> {
        return getCodeforcesApi(path = "user.rating") {
            parameter("handle", handle)
        }
    }

    //TODO: Sequence instead of List?
    suspend fun getContests(): List<CodeforcesContest> {
        return getCodeforcesApi(path = "contest.list") {
            parameter("gym", false)
        }
    }

    suspend fun getContestSubmissions(contestId: Int, handle: String): List<CodeforcesSubmission> {
        return getCodeforcesApi(path = "contest.status") {
            parameter("contestId", contestId)
            parameter("handle", handle)
            parameter("count", 1e9.toInt())
        }
    }

    suspend fun getUserSubmissions(handle: String, count: Long, from: Long): List<CodeforcesSubmission> {
        return getCodeforcesApi(path = "user.status") {
            parameter("handle", handle)
            parameter("count", count)
            parameter("from", from)
        }
    }

    //TODO: Sequence instead of List
    suspend fun getContestRatingChanges(contestId: Int): List<CodeforcesRatingChange> {
        return getCodeforcesApi(path = "contest.ratingChanges" ) {
            parameter("contestId", contestId)
        }
    }

    suspend fun getHandleSuggestionsPage(str: String): String {
        return getCodeforcesWeb(path = "data/handles") {
            parameter("q", str)
        }
    }

    suspend fun getPageSource(path: String, locale: CodeforcesLocale): String {
        return getCodeforcesWeb(path = path) {
            parameter("locale", locale)
        }
    }

    suspend fun getUserPage(handle: String): String {
        return getPageSource(path = urls.user(handle), locale = CodeforcesLocale.EN)
    }

    suspend fun getContestPage(contestId: Int): String {
        return getPageSource(path = urls.contest(contestId), locale = CodeforcesLocale.EN)
    }

    suspend fun getUserBlogEntries(handle: String, locale: CodeforcesLocale): List<CodeforcesBlogEntry> {
        return getCodeforcesApi(path = "user.blogEntries") {
            parameter("handle", handle)
            parameter("locale", locale)
        }
    }

    suspend fun getBlogEntry(blogEntryId: Int, locale: CodeforcesLocale): CodeforcesBlogEntry {
        return getCodeforcesApi(path = "blogEntry.view") {
            parameter("blogEntryId", blogEntryId)
            parameter("locale", locale)
        }
    }

    suspend fun getContestStandings(contestId: Int, handles: Collection<String>, includeUnofficial: Boolean): CodeforcesContestStandings {
        return getCodeforcesApi(path = "contest.standings") {
            parameter("contestId", contestId)
            parameter("handles", handles.joinToString(separator = ";"))
            parameter("showUnofficial", includeUnofficial)
        }
    }

    suspend fun getContestStandings(contestId: Int, handle: String, includeUnofficial: Boolean): CodeforcesContestStandings {
        return getContestStandings(contestId, listOf(handle), includeUnofficial)
    }


    object urls {
        const val main = "https://codeforces.com"

        fun user(handle: String) = "$main/profile/$handle"

        fun blogEntry(blogEntryId: Int) = "$main/blog/entry/$blogEntryId"

        fun comment(blogEntryId: Int, commentId: Long) = blogEntry(blogEntryId) + "#comment-$commentId"

        fun contest(contestId: Int) = "$main/contest/$contestId"

        fun contestPending(contestId: Int) = "$main/contests/$contestId"

        fun contestsWith(handle: String) = "$main/contests/with/$handle"

        fun submission(submission: CodeforcesSubmission) = "$main/contest/${submission.contestId}/submission/${submission.id}"

        fun problem(contestId: Int, problemIndex: String) = "$main/contest/$contestId/problem/$problemIndex"
    }
}


abstract class CodeforcesApiException(message: String?): Throwable(message) {
    constructor(): this(null)
}

internal enum class CodeforcesAPIStatus {
    OK, FAILED
}

@Serializable
private data class CodeforcesAPIResponse<T>(
    val status: CodeforcesAPIStatus,
    val result: T
)


private fun isTemporarilyUnavailable(str: String): Boolean {
    if (str.length > 2000) return false //trick based on full msg length
    val i = str.indexOf("<body>")
    if (i == -1) return false
    val j = str.lastIndexOf("</body>")
    if (j == -1 || i >= j) return false
    val pi = str.indexOf("<p>", i)
    val pj = str.lastIndexOf("</p>", j)
    if (pi == -1 || pj == -1 || pi > pj) return false
    val body = str.substring(pi + 3, pj)
    return body == "Codeforces is temporarily unavailable. Please, return in several minutes. Please try <a href=\"https://m1.codeforces.com/\">m1.codeforces.com</a>, <a href=\"https://m2.codeforces.com/\">m2.codeforces.com</a> or <a href=\"https://m3.codeforces.com/\">m3.codeforces.com</a>"
    /* full message:
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <title>Codeforces</title>
</head>
<body>
    <p>Codeforces is temporarily unavailable. Please, return in several minutes. Please try <a href="https://m1.codeforces.com/">m1.codeforces.com</a>, <a href="https://m2.codeforces.com/">m2.codeforces.com</a> or <a href="https://m3.codeforces.com/">m3.codeforces.com</a></p>
<script>(function(){var js = "window['__CF$cv$params']={r:'7f71322edbeb9d70',t:'MTY5MjA5OTk3NS41NTQwMDA='};_cpo=document.createElement('script');_cpo.nonce='',_cpo.src='/cdn-cgi/challenge-platform/scripts/invisible.js',document.getElementsByTagName('head')[0].appendChild(_cpo);";var _0xh = document.createElement('iframe');_0xh.height = 1;_0xh.width = 1;_0xh.style.position = 'absolute';_0xh.style.top = 0;_0xh.style.left = 0;_0xh.style.border = 'none';_0xh.style.visibility = 'hidden';document.body.appendChild(_0xh);function handler() {var _0xi = _0xh.contentDocument || _0xh.contentWindow.document;if (_0xi) {var _0xj = _0xi.createElement('script');_0xj.innerHTML = js;_0xi.getElementsByTagName('head')[0].appendChild(_0xj);}}if (document.readyState !== 'loading') {handler();} else if (window.addEventListener) {document.addEventListener('DOMContentLoaded', handler);} else {var prev = document.onreadystatechange || function () {};document.onreadystatechange = function (e) {prev(e);if (document.readyState !== 'loading') {document.onreadystatechange = prev;handler();}};}})();</script></body>
</html>
     */
}

//TODO: is cloudflare (just a moment...)