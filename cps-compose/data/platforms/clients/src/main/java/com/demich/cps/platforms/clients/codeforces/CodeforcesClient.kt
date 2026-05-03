package com.demich.cps.platforms.clients.codeforces

import com.demich.cps.platforms.api.BuildConfig
import com.demich.cps.platforms.api.codeforces.CodeforcesApi
import com.demich.cps.platforms.api.codeforces.CodeforcesApiAccess
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
import com.demich.cps.platforms.clients.RateLimitPlugin
import com.demich.cps.platforms.clients.cpsHttpClient
import com.demich.cps.platforms.clients.defaultJson
import com.demich.cps.platforms.clients.parameterList
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
import korlibs.crypto.sha512
import kotlinx.serialization.Serializable
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class CodeforcesClient(
    val locale: CodeforcesLocale = EN,
    val apiAccess: CodeforcesApiAccess? = null
): CodeforcesApi, CodeforcesPageContentProvider {

    private suspend inline fun codeforcesRequest(
        signApiMethod: String? = null,
        block: HttpRequestBuilder.() -> Unit
    ): HttpResponse {
        return codeforcesHttpClient.getCheckPOW {
            block()
            parameter("locale", locale)
            if (signApiMethod != null) {
                requireNotNull(apiAccess) { "Codeforces api access is null" }
                parameter("apiKey", apiAccess.key)
                parameter("time", Clock.System.now().epochSeconds)
                parameter("apiSig", apiSig(
                    method = signApiMethod,
                    parameters = url.parameters.entries(),
                    secret = apiAccess.secret
                ))
            }
        }
    }

    private suspend inline fun <reified T> getApi(
        method: String,
        sign: Boolean = false,
        block: HttpRequestBuilder.() -> Unit = {}
    ): T {
        return codeforcesRequest(method.takeIf { sign }) {
            url.appendPathSegments("api", method)
            block()
        }.body<CodeforcesAPIResponse<T>>().result
    }

    private fun HttpRequestBuilder.parameterHandles(handles: Collection<String>?) =
        parameterList(key = "handles", value = handles, separator = ";")

    override suspend fun getBlogEntry(blogEntryId: Int): CodeforcesBlogEntry =
        getApi(method = "blogEntry.view") {
            parameter("blogEntryId", blogEntryId)
        }

    override suspend fun getContests(gym: Boolean?): List<CodeforcesContest> =
        getApi(method = "contest.list") {
            parameter("gym", gym)
        }

    override suspend fun getContestRatingChanges(contestId: Int): List<CodeforcesRatingChange> =
        getApi(method = "contest.ratingChanges" ) {
            parameter("contestId", contestId)
        }

    override suspend fun getContestStandings(
        contestId: Int,
        handles: Collection<String>?,
        showUnofficial: Boolean?,
        participantTypes: Collection<CodeforcesParticipationType>?
    ): CodeforcesContestStandings =
        getApi(method = "contest.standings") {
            parameter("contestId", contestId)
            parameterHandles(handles)
            parameter("showUnofficial", showUnofficial)
            parameterList("participantTypes", participantTypes)
        }

    override suspend fun getContestSubmissions(
        contestId: Int,
        handle: String?,
        from: Int?,
        count: Int?
    ): List<CodeforcesSubmission> =
        getApi(method = "contest.status") {
            parameter("contestId", contestId)
            parameter("handle", handle)
            parameter("from", from)
            parameter("count", count)
        }

    override suspend fun getRecentActions(maxCount: Int): List<CodeforcesRecentAction> =
        getApi(method = "recentActions") {
            parameter("maxCount", maxCount)
        }

    override suspend fun getUserBlogEntries(handle: String): List<CodeforcesBlogEntry> =
        getApi(method = "user.blogEntries") {
            parameter("handle", handle)
        }

    override suspend fun getUsers(
        handles: Collection<String>,
        checkHistoricHandles: Boolean
    ): List<CodeforcesUser> {
        if (handles.isEmpty()) return emptyList()
        return getApi(method = "user.info") {
            parameterHandles(handles)
            parameter("checkHistoricHandles", checkHistoricHandles)
        }
    }

    override suspend fun getUserRatingChanges(handle: String): List<CodeforcesRatingChange> =
        getApi(method = "user.rating") {
            parameter("handle", handle)
        }

    override suspend fun getUserSubmissions(
        handle: String,
        from: Long,
        count: Long
    ): List<CodeforcesSubmission> =
        getApi(method = "user.status") {
            parameter("handle", handle)
            parameter("from", from)
            parameter("count", count)
        }

    // raw pages methods
    private suspend inline fun getWebPage(
        path: String,
        block: HttpRequestBuilder.() -> Unit = {}
    ): String {
        return codeforcesRequest {
            url(path)
            block()
        }.bodyAsText()
    }

    override suspend fun getHandleSuggestionsPage(str: String) =
        getWebPage(path = "data/handles") {
            parameter("q", str)
        }

    override suspend fun getUserPage(handle: String) =
        getWebPage(path = CodeforcesUrls.user(handle))

    override suspend fun getContestPage(contestId: Int) =
        getWebPage(path = CodeforcesUrls.contest(contestId))

    override suspend fun getMainPage() =
        getWebPage(path = "")

    override suspend fun getRecentActionsPage() =
        getWebPage(path = "recent-actions")

    override suspend fun getTopBlogEntriesPage() =
        getWebPage(path = "top")

    override suspend fun getTopCommentsPage(days: Int) =
        getWebPage(path = "topComments") {
            parameter("days", days)
        }

    override suspend fun getGroupsPage() =
        getWebPage(path = "groups")
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

            handleResponseException { exception ->
                if (exception !is ResponseException) return@handleResponseException
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

private fun apiSig(
    method: String,
    parameters: Set<Map.Entry<String, List<String>>>,
    secret: String
): String {
    val rand = buildString(capacity = 6) {
        repeat(6) {
            append(Random.nextInt(10))
        }
    }

    val sortedParams = buildList {
        parameters.forEach { entry ->
            entry.value.forEach { value ->
                add(Pair(entry.key, value))
            }
        }
        sortWith(compareBy({ it.first }, { it.second }))
    }.joinToString(separator = "&") { (param, value) -> "${param}=${value}" }

    val str = "$rand/$method?$sortedParams#$secret"

    return rand + str.toByteArray().sha512().hex
}

private enum class CodeforcesAPIStatus {
    OK, FAILED
}

@Serializable
private class CodeforcesAPIResponse<T>(
    //val status: CodeforcesAPIStatus,
    val result: T
)

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


//TODO: find proper solution (intercept / retry plugins not works)
private suspend inline fun HttpClient.getCheckPOW(block: HttpRequestBuilder.() -> Unit): HttpResponse {
    try {
        return get(block)
    } catch (exception: CodeforcesPOWException) {
        return get {
            cookie(name = "pow", value = proofOfWork(exception.pow))
            block()
        }
    }
}

private fun isBrowserChecker(str: String): Boolean {
    ifBetweenFirstFirst(str, "<p>", "</p") { msg ->
        return msg == "Please wait. Your browser is being checked. It may take a few seconds..."
    }
    return false
}

private class CodeforcesPOWException(val pow: String): Throwable("pow = $pow")

private fun proofOfWork(pow: String): String {
    repeat(Int.MAX_VALUE) {
        val str = "${it}_$pow"
        val h = str.toByteArray().sha1().hex
        if (h.startsWith("0000")) return str
    }
    throw IllegalStateException()
}