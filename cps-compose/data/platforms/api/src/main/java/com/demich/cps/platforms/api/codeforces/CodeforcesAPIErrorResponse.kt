package com.demich.cps.platforms.api.codeforces

import kotlinx.serialization.Serializable

@Serializable
class CodeforcesAPIErrorResponse internal constructor(
    private val comment: String
): CodeforcesApiException(comment) {

    internal fun mapOrThis(): CodeforcesApiException {
        if (isCallLimitExceeded()) return CodeforcesApiCallLimitExceededException(comment)

        return this
    }

    private fun isCallLimitExceeded() = comment == "Call limit exceeded"

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