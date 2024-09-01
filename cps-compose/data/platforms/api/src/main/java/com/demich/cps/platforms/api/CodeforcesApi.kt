package com.demich.cps.platforms.api

import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
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
                    .onSuccess { throw it }
                    .onFailure { throw exception }
            }
        }
    }

    class CodeforcesTemporarilyUnavailableException: CodeforcesApiException("Codeforces Temporarily Unavailable")

    private val callLimitExceededWaitTime: Duration = 500.milliseconds
    private val redirectWaitTime: Duration = 300.milliseconds
    private fun isCallLimitExceeded(e: Throwable): Boolean {
        if (e is CodeforcesAPIErrorResponse) return e.isCallLimitExceeded()
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

        private var rcpc_value: String = ""

        override fun toString(): String = rcpc_value

        private var last_c = ""
        fun recalc(source: String) {
            val i = source.indexOf("c=toNumbers(")
            val c = source.substring(source.indexOf("(\"",i)+2, source.indexOf("\")",i))
            if (c == last_c) return
            rcpc_value = decodeAES(c)
            last_c = c
            println("$c: $rcpc_value")
        }
    }

    private suspend fun getCodeforcesWeb(
        path: String,
        block: HttpRequestBuilder.() -> Unit = {}
    ): String {
        val callGet = suspend {
            client.getText(path) {
                header("Cookie", "RCPC=$RCPC")
                block()
            }.also {
                //TODO: check this for api requests too (in validateResponse)
                if (isTemporarilyUnavailable(it)) throw CodeforcesTemporarilyUnavailableException()
            }
        }
        val s = callGet()
        return if (s.startsWith("<html><body>Redirecting... Please, wait.")) {
            RCPC.recalc(s)
            delay(redirectWaitTime)
            callGet()
        } else s
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
        return getCodeforcesApi(path ="contest.ratingChanges" ) {
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


enum class CodeforcesLocale {
    EN, RU;

    override fun toString(): String =
        when(this) {
            EN -> "en"
            RU -> "ru"
        }
}

enum class CodeforcesColorTag {
    BLACK,
    GRAY,
    GREEN,
    CYAN,
    BLUE,
    VIOLET,
    ORANGE,
    RED,
    LEGENDARY,
    ADMIN;

    companion object {
        fun fromRating(rating: Int?): CodeforcesColorTag =
            when {
                rating == null -> BLACK
                rating < 1200 -> GRAY
                rating < 1400 -> GREEN
                rating < 1600 -> CYAN
                rating < 1900 -> BLUE
                rating < 2100 -> VIOLET
                rating < 2400 -> ORANGE
                rating < 3000 -> RED
                else -> LEGENDARY
            }
    }
}


abstract class CodeforcesApiException(message: String?): Throwable(message) {
    constructor(): this(null)
}

enum class CodeforcesAPIStatus {
    OK, FAILED
}

@Serializable
data class CodeforcesAPIErrorResponse(
    private val status: CodeforcesAPIStatus,
    private val comment: String
): CodeforcesApiException("Codeforces API: $comment") {

    fun isCallLimitExceeded() = comment == "Call limit exceeded"

    fun isHandleNotFound(): String? {
        val cut = comment.removeSurrounding("handles: User with handle ", " not found")
        if (cut == comment) return null
        return cut
    }

    fun isBlogEntryNotFound(blogEntryId: Int): Boolean {
        if (comment == "blogEntryId: Blog entry with id $blogEntryId not found") return true
        if (comment == "Blog entry with id $blogEntryId not found") return true
        return false
    }

    fun isBlogHandleNotFound(handle: String): Boolean {
        if (comment == "handle: User with handle $handle not found") return true
        if (comment == "handle: Field should contain between 3 and 24 characters, inclusive") return true
        if (comment == "handle: Поле должно содержать от 3 до 24 символов, включительно") return true
        return false
    }

    fun isNotAllowedToReadThatBlog(): Boolean {
        if (comment == "handle: You are not allowed to read that blog") return true
        return false
    }

    fun isContestRatingUnavailable(): Boolean {
        if (comment == "contestId: Rating changes are unavailable for this contest") return true
        return false
    }

    fun isContestNotStarted(contestId: Int): Boolean {
        if (comment == "contestId: Contest with id $contestId has not started") return true
        return false
    }
}

@Serializable
private data class CodeforcesAPIResponse<T>(
    val status: CodeforcesAPIStatus,
    val result: T
)

@Serializable
data class CodeforcesUser(
    val handle: String,
    val rating: Int? = null,
    val contribution: Int = 0,
    @SerialName("lastOnlineTimeSeconds")
    @Serializable(with = InstantAsSecondsSerializer::class)
    val lastOnlineTime: Instant = Instant.DISTANT_PAST
)

@Serializable
data class CodeforcesContest(
    val id: Int,
    val name: String,
    val phase: CodeforcesContestPhase,
    val type: CodeforcesContestType,
    @SerialName("durationSeconds")
    @Serializable(with = DurationAsSecondsSerializer::class)
    val duration: Duration,
    @SerialName("startTimeSeconds")
    @Serializable(with = InstantAsSecondsSerializer::class)
    val startTime: Instant
)

@Serializable
data class CodeforcesContestStandings(
    val contest: CodeforcesContest,
    val problems: List<CodeforcesProblem>,
    val rows: List<CodeforcesContestStandingsRow>
) {
    @Serializable
    data class CodeforcesContestStandingsRow(
        val rank: Int,
        val points: Double,
        val party: CodeforcesContestParticipant,
        val problemResults: List<CodeforcesProblemResult>
    )

    @Serializable
    data class CodeforcesContestParticipant(
        val contestId: Int,
        val participantType: CodeforcesParticipationType,
        val members: List<CodeforcesUser>
    )
}

@Serializable
data class CodeforcesProblem(
    val name: String,
    val index: String,
    val contestId: Int = -1,
    val points: Double = 0.0
) {
    val problemId: String get() = "$contestId$index"
}

@Serializable
data class CodeforcesProblemResult(
    val points: Double,
    val type: CodeforcesProblemStatus,
    val rejectedAttemptCount: Int
)

@Serializable
data class CodeforcesSubmission(
    val contestId: Int,
    val problem: CodeforcesProblem,
    val author: CodeforcesContestStandings.CodeforcesContestParticipant,
    val verdict: CodeforcesProblemVerdict = CodeforcesProblemVerdict.WAITING,
    val passedTestCount: Int,
    val id: Long,
    @SerialName("creationTimeSeconds")
    @Serializable(with = InstantAsSecondsSerializer::class)
    val creationTime: Instant,
    val testset: CodeforcesTestset
) {
    fun makeVerdict(): String {
        if (verdict == CodeforcesProblemVerdict.OK) return "OK"
        return "${verdict.name} #${passedTestCount+1}"
    }
}


@Serializable
data class CodeforcesBlogEntry(
    val id: Int,
    val title: String,
    val authorHandle: String,
    @SerialName("creationTimeSeconds")
    @Serializable(with = InstantAsSecondsSerializer::class)
    val creationTime: Instant,
    val rating: Int = 0,
    val commentsCount: Int = 0,
    val authorColorTag: CodeforcesColorTag = CodeforcesColorTag.BLACK
)

@Serializable
data class CodeforcesRatingChange(
    val contestId: Int,
    val contestName: String,
    val handle: String,
    val rank: Int,
    val oldRating: Int,
    val newRating: Int,
    @SerialName("ratingUpdateTimeSeconds")
    @Serializable(with = InstantAsSecondsSerializer::class)
    val ratingUpdateTime: Instant
)

@Serializable
data class CodeforcesComment(
    val id: Long,
    @SerialName("creationTimeSeconds")
    @Serializable(with = InstantAsSecondsSerializer::class)
    val creationTime: Instant,
    val commentatorHandle: String,
    val html: String,
    val rating: Int,
    val commentatorHandleColorTag: CodeforcesColorTag = CodeforcesColorTag.BLACK
)

@Serializable
data class CodeforcesRecentAction(
    @SerialName("timeSeconds")
    @Serializable(with = InstantAsSecondsSerializer::class)
    val time: Instant,
    val blogEntry: CodeforcesBlogEntry? = null,
    val comment: CodeforcesComment
)

enum class CodeforcesContestPhase {
    UNDEFINED,
    BEFORE,
    CODING,
    PENDING_SYSTEM_TEST,
    SYSTEM_TEST,
    FINISHED;

    val title: String
        get() = when (this) {
            PENDING_SYSTEM_TEST -> "PENDING SYSTEM TESTING"
            SYSTEM_TEST -> "SYSTEM TESTING"
            else -> name
        }

    fun isSystemTestOrFinished() =
        this == SYSTEM_TEST || this == FINISHED
}

enum class CodeforcesContestType {
    UNDEFINED,
    CF, ICPC, IOI
}

enum class CodeforcesParticipationType {
    NOT_PARTICIPATED,
    CONTESTANT, PRACTICE, VIRTUAL, MANAGER, OUT_OF_COMPETITION;

    fun contestParticipant(): Boolean = (this == CONTESTANT || this == OUT_OF_COMPETITION)
}

enum class CodeforcesProblemStatus {
    FINAL, PRELIMINARY
}

enum class CodeforcesProblemVerdict {
    WAITING,
    FAILED, OK, PARTIAL, COMPILATION_ERROR, RUNTIME_ERROR, WRONG_ANSWER, PRESENTATION_ERROR, TIME_LIMIT_EXCEEDED, MEMORY_LIMIT_EXCEEDED, IDLENESS_LIMIT_EXCEEDED, SECURITY_VIOLATED, CRASHED, INPUT_PREPARATION_CRASHED, CHALLENGED,
    SKIPPED, TESTING, REJECTED
    ;

    fun isResult(): Boolean = (this != WAITING && this != TESTING && this != SKIPPED)
}

enum class CodeforcesTestset {
    SAMPLES, PRETESTS, TESTS, CHALLENGES,
    TESTS1, TESTS2, TESTS3, TESTS4, TESTS5, TESTS6, TESTS7, TESTS8, TESTS9, TESTS10
}




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