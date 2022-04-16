package com.demich.cps.utils.codeforces

import com.demich.cps.accounts.managers.NOT_RATED
import com.demich.cps.utils.DurationAsSecondsSerializer
import com.demich.cps.utils.InstantAsSecondsSerializer
import com.demich.cps.utils.cpsHttpClient
import com.demich.cps.utils.jsonCPS
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlin.time.Duration

object CodeforcesAPI {
    private val client = cpsHttpClient {
        HttpResponseValidator {
            handleResponseException { exception ->
                if (exception !is ResponseException) return@handleResponseException
                val exceptionResponse = exception.response
                val text = exceptionResponse.readText()
                if(exceptionResponse.status == HttpStatusCode.ServiceUnavailable) {
                    throw CodeforcesAPICallLimitExceeded()
                }
                val codeforcesError = jsonCPS.decodeFromString<CodeforcesAPIErrorResponse>(text)
                throw codeforcesError
            }
        }
    }

    private const val callLimitExceededWaitTimeMillis: Long = 500
    class CodeforcesAPICallLimitExceeded: Throwable()

    private suspend fun<T> makeAPICall(block: suspend () -> CodeforcesAPIResponse<T>): T {
        repeat(10) { iteration ->
            kotlin.runCatching {
                withContext(Dispatchers.IO) {
                    block()
                }
            }.onSuccess {
                return it.result
            }.onFailure { exception ->
                if (exception is CodeforcesAPICallLimitExceeded) {
                    delay(callLimitExceededWaitTimeMillis)
                } else {
                    throw exception
                }
            }
        }
        throw CodeforcesAPICallLimitExceeded()
    }

    suspend fun getUsers(handles: Collection<String>): List<CodeforcesUser> = makeAPICall {
        client.get(urlString = "https://codeforces.com/api/user.info") {
            parameter("handles", handles.joinToString(separator = ";"))
        }
    }

    suspend fun getUser(handle: String) = getUsers(listOf(handle)).first()

    suspend fun getUserRatingChanges(handle: String): List<CodeforcesRatingChange> = makeAPICall {
        client.get(urlString = "https://codeforces.com/api/user.rating") {
            parameter("handle", handle)
        }
    }

    private suspend fun makeWEBCall(block: suspend () -> String): String? {
        //TODO: RCPC retry
        return kotlin.runCatching {
            withContext(Dispatchers.IO) {
                block()
            }
        }.getOrNull()
    }

    suspend fun getHandleSuggestions(str: String) = makeWEBCall {
        client.get(urlString = "https://codeforces.com/data/handles") {
            parameter("q", str)
        }
    }

    suspend fun getPageSource(page: String, locale: CodeforcesLocale) = makeWEBCall {
        client.get(urlString = page) {
            parameter("locale", locale)
        }
    }

    object URLFactory {
        const val main = "https://codeforces.com"

        fun user(handle: String) = "$main/profile/$handle"

        fun blog(blogId: Int) = "$main/blog/entry/$blogId"

        fun comment(blogId: Int, commentId: Long) = blog(blogId) + "#comment-$commentId"

        fun contest(contestId: Int) = "$main/contest/$contestId"

        fun contestOuter(contestId: Int) = "$main/contests/$contestId"

        fun contestsWith(handle: String) = "$main/contests/with/$handle"

        //fun submission(submission: CodeforcesSubmission) = "$main/contest/${submission.contestId}/submission/${submission.id}"

        fun problem(contestId: Int, problemIndex: String) = "$main/contest/$contestId/problem/$problemIndex"
    }
}



enum class CodeforcesAPIStatus{
    OK, FAILED
}

@Serializable
data class CodeforcesAPIErrorResponse(
    val status: CodeforcesAPIStatus,
    val comment: String
): Throwable(comment) {
    fun isCallLimitExceeded() = comment == "Call limit exceeded"

    fun isHandleNotFound(): String? {
        val cut = comment.removeSurrounding("handles: User with handle ", " not found")
        if(cut == comment) return null
        return cut
    }

    fun isBlogNotFound(blogId: Int): Boolean {
        if(comment == "blogEntryId: Blog entry with id $blogId not found") return true
        if(comment == "Blog entry with id $blogId not found") return true
        return false
    }

    fun isBlogHandleNotFound(handle: String): Boolean {
        if(comment == "handle: User with handle $handle not found") return true
        if(comment == "handle: Field should contain between 3 and 24 characters, inclusive") return true
        if(comment == "handle: Поле должно содержать от 3 до 24 символов, включительно") return true
        return false
    }

    fun isNotAllowedToReadThatBlog(): Boolean {
        if(comment == "handle: You are not allowed to read that blog") return true
        return false
    }

    fun isContestRatingUnavailable(): Boolean {
        if(comment == "contestId: Rating changes are unavailable for this contest") return true
        return false
    }

    fun isContestNotStarted(contestId: Int): Boolean {
        if(comment == "contestId: Contest with id $contestId has not started") return true
        return false
    }
}

@Serializable
data class CodeforcesAPIResponse<T>(
    val status: CodeforcesAPIStatus,
    val result: T
)

@Serializable
data class CodeforcesUser(
    val handle: String,
    val rating: Int = NOT_RATED,
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
    val startTime: Instant,
    val relativeTimeSeconds: Long
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
)

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
        if(verdict == CodeforcesProblemVerdict.OK) return "OK"
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
    val authorColorTag: CodeforcesUtils.ColorTag = CodeforcesUtils.ColorTag.BLACK
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
    val text: String,
    val rating: Int,
    val commentatorHandleColorTag: CodeforcesUtils.ColorTag = CodeforcesUtils.ColorTag.BLACK
)

@Serializable
data class CodeforcesRecentAction(
    @SerialName("timeSeconds")
    @Serializable(with = InstantAsSecondsSerializer::class)
    val time: Instant,
    val blogEntry: CodeforcesBlogEntry? = null,
    val comment: CodeforcesComment? = null
)

enum class CodeforcesContestPhase(private val title: String? = null) {
    UNDEFINED,
    BEFORE,
    CODING,
    PENDING_SYSTEM_TEST("PENDING SYSTEM TESTING"),
    SYSTEM_TEST("SYSTEM TESTING"),
    FINISHED;

    fun getTitle(): String = title ?: name

    fun isFutureOrRunning(): Boolean {
        return this != UNDEFINED && this != FINISHED
    }
}

enum class CodeforcesContestType {
    UNDEFINED,
    CF, ICPC, IOI
}

enum class CodeforcesParticipationType {
    NOT_PARTICIPATED,
    CONTESTANT, PRACTICE, VIRTUAL, MANAGER, OUT_OF_COMPETITION;

    fun participatedInContest(): Boolean = (this == CONTESTANT || this == OUT_OF_COMPETITION)
}

enum class CodeforcesProblemStatus {
    FINAL, PRELIMINARY
}

enum class CodeforcesProblemVerdict {
    WAITING,
    FAILED, OK, PARTIAL, COMPILATION_ERROR, RUNTIME_ERROR, WRONG_ANSWER, PRESENTATION_ERROR, TIME_LIMIT_EXCEEDED, MEMORY_LIMIT_EXCEEDED, IDLENESS_LIMIT_EXCEEDED, SECURITY_VIOLATED, CRASHED, INPUT_PREPARATION_CRASHED, CHALLENGED, SKIPPED, TESTING, REJECTED
    ;

    fun isTested(): Boolean = (this != WAITING && this != TESTING && this != SKIPPED)
}

enum class CodeforcesTestset {
    SAMPLES, PRETESTS, TESTS, CHALLENGES,
    TESTS1, TESTS2, TESTS3, TESTS4, TESTS5, TESTS6, TESTS7, TESTS8, TESTS9, TESTS10
}
