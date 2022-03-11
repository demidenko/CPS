package com.demich.cps.utils.codeforces

import com.demich.cps.accounts.NOT_RATED
import com.demich.cps.utils.InstantAsSecondsSerializer
import com.demich.cps.utils.cpsHttpClient
import com.demich.cps.utils.jsonCPS
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

object CodeforcesAPI {
    private val apiClient = cpsHttpClient {
        HttpResponseValidator {
            handleResponseException { exception ->
                if (exception !is ResponseException) return@handleResponseException
                val exceptionResponse = exception.response
                val text = exceptionResponse.readText()
                val codeforcesError by lazy { jsonCPS.decodeFromString<CodeforcesAPIErrorResponse>(text) }
                if(exceptionResponse.status == HttpStatusCode.ServiceUnavailable) {
                    throw CodeforcesAPICallLimitExceeded()
                }
                throw codeforcesError
            }
        }
    }

    private const val callLimitExceededWaitTimeMillis: Long = 500
    class CodeforcesAPICallLimitExceeded: Throwable()

    private suspend fun<T> makeAPICall(
        block: suspend () -> CodeforcesAPIResponse<T>
    ): T {
        repeat(10) { iteration ->
            kotlin.runCatching {
                block()
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
        apiClient.get(urlString = "https://codeforces.com/api/user.info") {
            parameter("handles", handles.joinToString(separator = ";"))
        }
    }

    suspend fun getUser(handle: String) = getUsers(listOf(handle)).first()
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