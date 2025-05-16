package com.demich.cps.platforms.api.codeforces

import com.demich.cps.platforms.api.BuildConfig
import com.demich.cps.platforms.api.RateLimitingSemaphore
import com.demich.cps.platforms.api.PlatformApi
import com.demich.cps.platforms.api.codeforces.models.CodeforcesBlogEntry
import com.demich.cps.platforms.api.codeforces.models.CodeforcesContest
import com.demich.cps.platforms.api.codeforces.models.CodeforcesContestStandings
import com.demich.cps.platforms.api.codeforces.models.CodeforcesLocale
import com.demich.cps.platforms.api.codeforces.models.CodeforcesRatingChange
import com.demich.cps.platforms.api.codeforces.models.CodeforcesRecentAction
import com.demich.cps.platforms.api.codeforces.models.CodeforcesSubmission
import com.demich.cps.platforms.api.codeforces.models.CodeforcesUser
import com.demich.cps.platforms.api.cpsHttpClient
import com.demich.cps.platforms.api.defaultJson
import com.demich.kotlin_stdlib_boost.ifBetweenFirstFirst
import com.demich.kotlin_stdlib_boost.ifBetweenFirstLast
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.cookie
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.appendPathSegments
import io.ktor.http.setCookie
import korlibs.crypto.sha1
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

object CodeforcesApi: PlatformApi {
    private val json get() = defaultJson

    override val client = cpsHttpClient(
        json = json,
        useCookies = true
    ) {
        defaultRequest {
            url(urls.main)
        }

        HttpResponseValidator {
            validateResponse { response ->
                if (response.status.value == 200) {
                    val text = response.bodyAsText()

                    if (isTemporarilyUnavailable(text)) {
                        throw CodeforcesTemporarilyUnavailableException()
                    }

                    if (isBrowserChecker(text)) {
                        response.setCookie().firstOrNull { it.name == "pow" }?.let {
                            val pow = it.value
                            val url = response.request.url
                            println("pow = $pow for $url")
                            throw CodeforcesPOWException(pow)
                        }
                    }
                }
            }
            handleResponseExceptionWithRequest { exception, _ ->
                if (exception !is ResponseException) return@handleResponseExceptionWithRequest
                val response = exception.response
                json.runCatching { decodeFromString<CodeforcesAPIErrorResponse>(response.bodyAsText()) }
                    .onSuccess { throw it.mapOrThis() }
                    .onFailure { throw exception }
            }
        }

        //careful!!! only one install and retry block is used in ktor
        install(HttpRequestRetry) {
            retryOnExceptionIf(maxRetries = 10) { requestBuilder, throwable ->
                throwable.shouldRetry()
            }
            delayMillis { callLimitExceededWaitTime.inWholeMilliseconds }
        }
    }

    class CodeforcesTemporarilyUnavailableException: CodeforcesApiException("Codeforces Temporarily Unavailable")
    private class CodeforcesPOWException(val pow: String): CodeforcesApiException("pow = $pow")

    private val callLimitExceededWaitTime: Duration get() = 500.milliseconds
    private fun Throwable.shouldRetry(): Boolean {
        if (this is ResponseException && response.status.value in 500 until 600) return true
        if (this is CodeforcesApiCallLimitExceededException) return true
        return false
    }

    private val semaphore = RateLimitingSemaphore(
        permits = 4,
        permitsPerSecond = if (BuildConfig.DEBUG) 1 else 3,
        minDelay = 50.milliseconds
    )

    private suspend inline fun getWithPermit(block: HttpRequestBuilder.() -> Unit) =
        semaphore.withPermit { client.get(block) }

    //TODO: find proper solution (intercept / retry plugins not works)
    private suspend inline fun getCodeforces(block: HttpRequestBuilder.() -> Unit): HttpResponse {
        runCatching {
            return getWithPermit(block)
        }.getOrElse { exception ->
            if (exception is CodeforcesPOWException) {
                return getWithPermit {
                    cookie(name = "pow", value = proofOfWork(exception.pow))
                    block()
                }
            }
            throw exception
        }
    }

    //api methods from https://codeforces.com/apiHelp/methods
    private suspend inline fun <reified T> getCodeforcesApi(
        method: String,
        block: HttpRequestBuilder.() -> Unit = {}
    ): T {
        return getCodeforces {
            url.appendPathSegments("api", method)
            block()
        }.body<CodeforcesAPIResponse<T>>().result
    }

    suspend fun getBlogEntry(blogEntryId: Int, locale: CodeforcesLocale): CodeforcesBlogEntry {
        return getCodeforcesApi(method = "blogEntry.view") {
            parameter("blogEntryId", blogEntryId)
            parameter("locale", locale)
        }
    }

    //TODO: Sequence instead of List?
    suspend fun getContests(): List<CodeforcesContest> {
        return getCodeforcesApi(method = "contest.list") {
            parameter("gym", false)
        }
    }

    //TODO: Sequence instead of List
    suspend fun getContestRatingChanges(contestId: Int): List<CodeforcesRatingChange> {
        return getCodeforcesApi(method = "contest.ratingChanges" ) {
            parameter("contestId", contestId)
        }
    }

    suspend fun getContestStandings(
        contestId: Int,
        handles: Collection<String>,
        includeUnofficial: Boolean
        //TODO: participantTypes: Collection<CodeforcesParticipationType>
    ): CodeforcesContestStandings {
        return getCodeforcesApi(method = "contest.standings") {
            parameter("contestId", contestId)
            parameter("handles", handles.joinToString(separator = ";"))
            parameter("showUnofficial", includeUnofficial)
        }
    }

    suspend fun getContestStandings(contestId: Int, handle: String, includeUnofficial: Boolean): CodeforcesContestStandings {
        return getContestStandings(contestId, listOf(handle), includeUnofficial)
    }

    suspend fun getContestSubmissions(contestId: Int, handle: String): List<CodeforcesSubmission> {
        return getCodeforcesApi(method = "contest.status") {
            parameter("contestId", contestId)
            parameter("handle", handle)
            parameter("count", 1e9.toInt())
        }
    }

    suspend fun getRecentActions(locale: CodeforcesLocale, maxCount: Int = Int.MAX_VALUE): List<CodeforcesRecentAction> {
        return getCodeforcesApi(method = "recentActions") {
            parameter("maxCount", maxCount.coerceIn(0, 100))
            parameter("locale", locale)
        }
    }

    suspend fun getUserBlogEntries(handle: String, locale: CodeforcesLocale): List<CodeforcesBlogEntry> {
        return getCodeforcesApi(method = "user.blogEntries") {
            parameter("handle", handle)
            parameter("locale", locale)
        }
    }

    suspend fun getUsers(
        handles: Collection<String>,
        checkHistoricHandles: Boolean = false
    ): List<CodeforcesUser> {
        if (handles.isEmpty()) return emptyList()
        return getCodeforcesApi(method = "user.info") {
            parameter("handles", handles.joinToString(separator = ";"))
            parameter("checkHistoricHandles", checkHistoricHandles)
        }
    }

    suspend fun getUser(handle: String, checkHistoricHandles: Boolean = false): CodeforcesUser {
        return getUsers(listOf(handle), checkHistoricHandles).first()
    }

    suspend fun getUserRatingChanges(handle: String): List<CodeforcesRatingChange> {
        return getCodeforcesApi(method = "user.rating") {
            parameter("handle", handle)
        }
    }

    suspend fun getUserSubmissions(handle: String, count: Long, from: Long): List<CodeforcesSubmission> {
        return getCodeforcesApi(method = "user.status") {
            parameter("handle", handle)
            parameter("count", count)
            parameter("from", from)
        }
    }

    //raw pages methods
    private suspend inline fun getCodeforcesPage(
        path: String,
        block: HttpRequestBuilder.() -> Unit = {}
    ): String {
        return getCodeforces {
            url(path)
            block()
        }.bodyAsText()
    }

    private suspend inline fun getCodeforcesPage(
        path: String,
        locale: CodeforcesLocale,
        block: HttpRequestBuilder.() -> Unit = {}
    ): String {
        return getCodeforcesPage(path = path) {
            parameter("locale", locale)
            block()
        }
    }

    suspend fun getPage(page: BasePage, locale: CodeforcesLocale): String {
        return getCodeforcesPage(path = page.path, locale = locale)
    }

    suspend fun getHandleSuggestionsPage(str: String): String {
        return getCodeforcesPage(path = "data/handles") {
            parameter("q", str)
        }
    }

    suspend fun getUserPage(handle: String): String {
        return getCodeforcesPage(path = urls.user(handle), locale = CodeforcesLocale.EN)
    }

    suspend fun getContestPage(contestId: Int): String {
        return getCodeforcesPage(path = urls.contest(contestId), locale = CodeforcesLocale.EN)
    }

    suspend fun getTopCommentsPage(locale: CodeforcesLocale, days: Int = 2): String {
        return getCodeforcesPage(path = "topComments", locale = locale) {
            parameter("days", days)
        }
    }

    object urls {
        const val main = "https://codeforces.com"

        fun user(handle: String) = "$main/profile/$handle"

        fun blogEntry(blogEntryId: Int) = "$main/blog/entry/$blogEntryId"

        fun comment(blogEntryId: Int, commentId: Long) = blogEntry(blogEntryId) + "#comment-$commentId"

        fun contest(contestId: Int) = "$main/contest/$contestId"

        fun contestPending(contestId: Int) = "$main/contests/$contestId"

        fun contestsWith(handle: String) = "$main/contests/with/$handle"

        fun submission(submission: CodeforcesSubmission) = contest(submission.contestId) + "/submission/${submission.id}"

        fun problem(contestId: Int, problemIndex: String) = contest(contestId) + "/problem/$problemIndex"
    }

    enum class BasePage(val path: String) {
        main(""),
        top("top"),
        recent("recent-actions"),
        groups("groups")
    }
}


abstract class CodeforcesApiException(message: String?): Throwable(message) {
    constructor(): this(null)
}

private enum class CodeforcesAPIStatus {
    OK, FAILED
}

@Serializable
private class CodeforcesAPIResponse<T>(
    //val status: CodeforcesAPIStatus,
    val result: T
)

private fun isBrowserChecker(str: String): Boolean {
    ifBetweenFirstFirst(str, "<p>", "</p") { msg ->
        return msg == "Please wait. Your browser is being checked. It may take a few seconds..."
    }
    return false
}

private fun isTemporarilyUnavailable(str: String): Boolean {
    if (str.length > 2000) return false //trick based on full msg length
    ifBetweenFirstLast(str, "<body>", "</body>") { body ->
        ifBetweenFirstLast(body, "<p>", "</p>") { msg ->
            return msg == "Codeforces is temporarily unavailable. Please, return in several minutes. Please try <a href=\"https://m1.codeforces.com/\">m1.codeforces.com</a>, <a href=\"https://m2.codeforces.com/\">m2.codeforces.com</a> or <a href=\"https://m3.codeforces.com/\">m3.codeforces.com</a>"
        }
    }
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
    return false
}

//TODO: is cloudflare (just a moment...)

private fun proofOfWork(pow: String): String {
    repeat(Int.MAX_VALUE) {
        val str = "${it}_$pow"
        val h = str.toByteArray().sha1().hex
        if (h.startsWith("0000")) return str
    }
    throw IllegalStateException()
}