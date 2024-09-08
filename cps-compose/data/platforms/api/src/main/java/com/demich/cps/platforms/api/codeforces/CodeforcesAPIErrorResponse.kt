package com.demich.cps.platforms.api.codeforces

import kotlinx.serialization.Serializable
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@Serializable
class CodeforcesAPIErrorResponse internal constructor(
    private val comment: String
): CodeforcesApiException(comment) {

    internal fun mapOrThis(): CodeforcesApiException {
        if (isCallLimitExceeded()) return CodeforcesApiCallLimitExceededException(comment)

        isHandleNotFound()?.let { handle -> return CodeforcesApiHandleNotFoundException(comment, handle) }

        if (isContestRatingUnavailable()) return CodeforcesApiContestRatingUnavailableException(comment)
        isContestNotStarted()?.let { contestId -> return CodeforcesApiContestNotStartedException(comment, contestId) }

        if (isNotAllowedToReadThatBlog()) return CodeforcesApiNotAllowedReadBlogException(comment)

        return this
    }

    private fun isCallLimitExceeded() = comment == "Call limit exceeded"

    private fun isHandleNotFound(): String? {
        //userinfo response
        comment.ifSurrounded("handles: User with handle ", " not found") { return it }

        //user blog response
        comment.ifSurrounded("handle: User with handle ", " not found") { return it }

        return null
    }

    fun isBlogEntryNotFound(blogEntryId: Int): Boolean {
        if (comment == "blogEntryId: Blog entry with id $blogEntryId not found") return true
        if (comment == "Blog entry with id $blogEntryId not found") return true
        return false
    }

    //user blog response
    private fun isHandleFieldIncorrectLength(): Boolean {
        if (comment == "handle: Field should contain between 3 and 24 characters, inclusive") return true
        if (comment == "handle: Поле должно содержать от 3 до 24 символов, включительно") return true
        return false
    }

    private fun isNotAllowedToReadThatBlog(): Boolean {
        if (comment == "handle: You are not allowed to read that blog") return true
        return false
    }

    private fun isContestRatingUnavailable(): Boolean {
        if (comment == "contestId: Rating changes are unavailable for this contest") return true
        return false
    }

    private fun isContestNotStarted(): Int? {
        comment.ifSurrounded("contestId: Contest with id ", " has not started") {
            return it.toIntOrNull()
        }
        return null
    }

    fun isContestNotFound(contestId: Int): Boolean {
        if (comment == "contestId: Contest with id $contestId not found") return true
        return false
    }

    fun isContestManagerAreNotAllowed(): Boolean {
        if (comment == "asManager: Only contest managers can use \"asManager\" option") return true
        return false
    }
}


class CodeforcesApiCallLimitExceededException
internal constructor(comment: String): CodeforcesApiException(comment)

class CodeforcesApiHandleNotFoundException
internal constructor(comment: String, val handle: String): CodeforcesApiException(comment)

class CodeforcesApiNotAllowedReadBlogException
internal constructor(comment: String): CodeforcesApiException(comment)

class CodeforcesApiContestRatingUnavailableException
internal constructor(comment: String): CodeforcesApiException(comment)

class CodeforcesApiContestNotStartedException
internal constructor(comment: String, val contestId: Int): CodeforcesApiException(comment)


@OptIn(ExperimentalContracts::class)
private inline fun String.ifSurrounded(prefix: String, suffix: String, block: (String) -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    if (startsWith(prefix) && endsWith(suffix)) {
        block(substring(startIndex = prefix.length, endIndex = length - suffix.length))
    }
}
