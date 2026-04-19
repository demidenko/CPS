package com.demich.cps.platforms.clients.codeforces

import com.demich.cps.platforms.api.BuildConfig
import com.demich.cps.platforms.api.codeforces.CodeforcesApi
import com.demich.cps.platforms.api.codeforces.CodeforcesApiCallLimitExceededException
import com.demich.cps.platforms.api.codeforces.CodeforcesPageContentProvider
import com.demich.cps.platforms.api.codeforces.CodeforcesTemporarilyUnavailableException
import com.demich.cps.platforms.api.codeforces.CodeforcesUrls
import com.demich.cps.platforms.api.codeforces.models.CodeforcesBlogEntry
import com.demich.cps.platforms.api.codeforces.models.CodeforcesContest
import com.demich.cps.platforms.api.codeforces.models.CodeforcesContestStandings
import com.demich.cps.platforms.api.codeforces.models.CodeforcesLocale
import com.demich.cps.platforms.api.codeforces.models.CodeforcesParticipationType
import com.demich.cps.platforms.api.codeforces.models.CodeforcesRatingChange
import com.demich.cps.platforms.api.codeforces.models.CodeforcesRecentAction
import com.demich.cps.platforms.api.codeforces.models.CodeforcesSubmission
import com.demich.cps.platforms.api.codeforces.models.CodeforcesUser
import com.demich.cps.platforms.clients.PlatformClient
import com.demich.cps.platforms.clients.RateLimitPlugin
import com.demich.cps.platforms.clients.cpsHttpClient
import com.demich.cps.platforms.clients.defaultJson
import com.demich.kotlin_stdlib_boost.ifBetweenFirstFirst
import io.ktor.client.HttpClient
import io.ktor.client.call.body
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
import io.ktor.http.HttpStatusCode
import io.ktor.http.appendPathSegments
import io.ktor.http.setCookie
import korlibs.crypto.sha1
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class CodeforcesClient(
    val locale: CodeforcesLocale = EN
): PlatformClient, CodeforcesApi, CodeforcesPageContentProvider {
    override val client get() = codeforcesHttpClient

    //TODO: find proper solution (intercept / retry plugins not works)
    private suspend inline fun getCodeforces(block: HttpRequestBuilder.() -> Unit): HttpResponse {
        try {
            return client.get(block)
        } catch (exception: CodeforcesPOWException) {
            return client.get {
                cookie(name = "pow", value = proofOfWork(exception.pow))
                block()
            }
        }
    }

    private suspend inline fun <reified T> getCodeforcesApi(
        method: String,
        block: HttpRequestBuilder.() -> Unit = {}
    ): T {
        return getCodeforces {
            url.appendPathSegments("api", method)
            block()
            parameter("locale", locale)
        }.body<CodeforcesAPIResponse<T>>().result
    }

    private fun HttpRequestBuilder.handles(handles: Collection<String>) {
        parameter(key = "handles", value = handles.joinToString(separator = ";"))
    }

    override suspend fun getBlogEntry(blogEntryId: Int): CodeforcesBlogEntry =
        getCodeforcesApi(method = "blogEntry.view") {
            parameter("blogEntryId", blogEntryId)
        }

    override suspend fun getContests(): List<CodeforcesContest> =
        getCodeforcesApi(method = "contest.list") {
            parameter("gym", false)
        }

    override suspend fun getContestRatingChanges(contestId: Int): List<CodeforcesRatingChange> =
        getCodeforcesApi(method = "contest.ratingChanges" ) {
            parameter("contestId", contestId)
        }

    private suspend inline fun getContestStandings(
        contestId: Int,
        handles: Collection<String>,
        block: HttpRequestBuilder.() -> Unit = {}
    ): CodeforcesContestStandings =
        getCodeforcesApi(method = "contest.standings") {
            parameter("contestId", contestId)
            handles(handles)
            block()
        }

    override suspend fun getContestStandings(
        contestId: Int,
        handles: Collection<String>,
        includeUnofficial: Boolean
    ): CodeforcesContestStandings =
        getContestStandings(contestId = contestId, handles = handles) {
            parameter("showUnofficial", includeUnofficial)
        }

    override suspend fun getContestStandings(
        contestId: Int,
        handles: Collection<String>,
        participantTypes: Collection<CodeforcesParticipationType>
    ): CodeforcesContestStandings =
        getContestStandings(contestId = contestId, handles = handles) {
            parameter("participantTypes", participantTypes.joinToString(separator = ","))
        }

    override suspend fun getContestSubmissions(contestId: Int, handle: String): List<CodeforcesSubmission> =
        getCodeforcesApi(method = "contest.status") {
            parameter("contestId", contestId)
            parameter("handle", handle)
            parameter("count", 1e9.toInt())
        }

    override suspend fun getRecentActions(maxCount: Int): List<CodeforcesRecentAction> =
        getCodeforcesApi(method = "recentActions") {
            parameter("maxCount", maxCount.coerceIn(0, 100))
        }

    override suspend fun getUserBlogEntries(handle: String): List<CodeforcesBlogEntry> =
        getCodeforcesApi(method = "user.blogEntries") {
            parameter("handle", handle)
        }

    override suspend fun getUsers(
        handles: Collection<String>,
        checkHistoricHandles: Boolean
    ): List<CodeforcesUser> {
        if (handles.isEmpty()) return emptyList()
        return getCodeforcesApi(method = "user.info") {
            handles(handles)
            parameter("checkHistoricHandles", checkHistoricHandles)
        }
    }

    override suspend fun getUserRatingChanges(handle: String): List<CodeforcesRatingChange> =
        getCodeforcesApi(method = "user.rating") {
            parameter("handle", handle)
        }

    override suspend fun getUserSubmissions(handle: String, count: Long, from: Long): List<CodeforcesSubmission> =
        getCodeforcesApi(method = "user.status") {
            parameter("handle", handle)
            parameter("count", count)
            parameter("from", from)
        }

    // raw pages methods
    private suspend inline fun getCodeforcesPage(
        path: String,
        block: HttpRequestBuilder.() -> Unit = {}
    ): String {
        return getCodeforces {
            url(path)
            block()
            parameter("locale", locale)
        }.bodyAsText()
    }

    override suspend fun getHandleSuggestionsPage(str: String) =
        getCodeforcesPage(path = "data/handles") {
            parameter("q", str)
        }

    override suspend fun getUserPage(handle: String) =
        getCodeforcesPage(path = CodeforcesUrls.user(handle))

    override suspend fun getContestPage(contestId: Int) =
        getCodeforcesPage(path = CodeforcesUrls.contest(contestId))

    override suspend fun getMainPage() =
        getCodeforcesPage(path = "")

    override suspend fun getRecentActionsPage() =
        getCodeforcesPage(path = "recent-actions")

    override suspend fun getTopBlogEntriesPage() =
        getCodeforcesPage(path = "top")

    override suspend fun getTopCommentsPage(days: Int) =
        getCodeforcesPage(path = "topComments") {
            parameter("days", days)
        }

    override suspend fun getGroupsPage() =
        getCodeforcesPage(path = "groups")
}

private val codeforcesHttpClient: HttpClient =
    cpsHttpClient(
        json = defaultJson,
        useCookies = true,
        retryOnExceptionIf = { it is CodeforcesApiCallLimitExceededException }
    ) {
        defaultRequest {
            url(CodeforcesUrls.main)
        }

        HttpResponseValidator {
            validateResponse { response ->
                if (response.status == HttpStatusCode.OK) {
                    val text = response.bodyAsText()

                    if (isTemporarilyUnavailable(text)) {
                        throw CodeforcesTemporarilyUnavailableException()
                    }

                    //TODO: rework this to plugin
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
                runCatching { response.body<CodeforcesAPIErrorResponse>() }
                    .onSuccess { throw it.toApiException() }
            }
        }

        install(RateLimitPlugin) {
            if (BuildConfig.DEBUG) {
                1 per 1.seconds
            } else {
                3 per 1.seconds
            }
            1 per 50.milliseconds
        }
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
    ifBetweenFirstFirst(str, "<p>", "</p>") { msg ->
        return msg.startsWith("Codeforces is temporarily unavailable. Please, return in several minutes. Please try")
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


private class CodeforcesPOWException(val pow: String): Throwable("pow = $pow")

private fun proofOfWork(pow: String): String {
    repeat(Int.MAX_VALUE) {
        val str = "${it}_$pow"
        val h = str.toByteArray().sha1().hex
        if (h.startsWith("0000")) return str
    }
    throw IllegalStateException()
}