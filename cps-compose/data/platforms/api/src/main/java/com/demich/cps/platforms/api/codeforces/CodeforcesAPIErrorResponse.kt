package com.demich.cps.platforms.api.codeforces

import kotlinx.serialization.Serializable

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
