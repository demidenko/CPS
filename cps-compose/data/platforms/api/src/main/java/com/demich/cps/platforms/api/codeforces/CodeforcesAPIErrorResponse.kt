package com.demich.cps.platforms.api.codeforces

import kotlinx.serialization.Serializable

@Serializable
internal class CodeforcesAPIErrorResponse(val comment: String) {

    fun toApiException(): CodeforcesApiException {
        if (isCallLimitExceeded()) return CodeforcesApiCallLimitExceededException(comment)

        ifIsHandleNotFound { return CodeforcesApiHandleNotFoundException(comment, handle = it) }

        if (isContestRatingUnavailable()) return CodeforcesApiContestRatingUnavailableException(comment)
        ifIsContestNotStarted { return CodeforcesApiContestNotStartedException(comment, contestId = it) }
        ifIsContestNotFound { return CodeforcesApiContestNotFoundException(comment, contestId = it) }

        if (isNotAllowedToReadThatBlog()) return CodeforcesApiNotAllowedReadBlogException(comment)

        return CodeforcesApiException(comment)
    }

    private fun isCallLimitExceeded() = comment == "Call limit exceeded"

    private inline fun ifIsHandleNotFound(block: (String) -> Unit) {
        //userinfo response
        comment.ifSurrounded("handles: User with handle ", " not found", block)

        //user blog response
        comment.ifSurrounded("handle: User with handle ", " not found", block)
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

    private inline fun ifIsContestNotStarted(block: (Int) -> Unit) {
        comment.ifIntSurrounded("contestId: Contest with id ", " has not started", block)
    }

    private inline fun ifIsContestNotFound(block: (Int) -> Unit) {
        comment.ifIntSurrounded("contestId: Contest with id ", " not found", block)
    }

    private fun isContestManagerAreNotAllowed(): Boolean {
        if (comment == "asManager: Only contest managers can use \"asManager\" option") return true
        return false
    }

    private inline fun ifIsBlogEntryNotFound(block: (Int) -> Unit) {
        comment.ifIntSurrounded("blogEntryId: Blog entry with id ", " not found", block)
        comment.ifIntSurrounded("Blog entry with id ", " not found", block)
    }
}

private inline fun String.ifSurrounded(prefix: String, suffix: String, block: (String) -> Unit) {
    if (prefix.length + suffix.length <= length && startsWith(prefix) && endsWith(suffix)) {
        block(substring(startIndex = prefix.length, endIndex = length - suffix.length))
    }
}

private inline fun String.ifIntSurrounded(prefix: String, suffix: String, block: (Int) -> Unit) {
    ifSurrounded(prefix = prefix, suffix = suffix) {
        it.toIntOrNull()?.let(block)
    }
}