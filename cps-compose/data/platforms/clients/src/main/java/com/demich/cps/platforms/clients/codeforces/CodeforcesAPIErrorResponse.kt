package com.demich.cps.platforms.clients.codeforces

import com.demich.cps.platforms.api.codeforces.CodeforcesApiBlogEntryNotFoundException
import com.demich.cps.platforms.api.codeforces.CodeforcesApiBlogReadNotAllowedException
import com.demich.cps.platforms.api.codeforces.CodeforcesApiCallLimitExceededException
import com.demich.cps.platforms.api.codeforces.CodeforcesApiContestNotFoundException
import com.demich.cps.platforms.api.codeforces.CodeforcesApiContestNotStartedException
import com.demich.cps.platforms.api.codeforces.CodeforcesApiContestRatingChangesUnavailableException
import com.demich.cps.platforms.api.codeforces.CodeforcesApiException
import com.demich.cps.platforms.api.codeforces.CodeforcesApiHandleNotFoundException
import com.demich.cps.platforms.api.codeforces.CodeforcesApiUnspecifiedException
import kotlinx.serialization.Serializable

@Serializable
internal class CodeforcesAPIErrorResponse(val comment: String) {

    /* example:
    {
        "status": "FAILED",
        "comment": "contestId: Contest with id 0 not found"
    }
     */

    /* TODO:

    cf can answer multiple comments
    https://codeforces.com/api/user.status?handle=&from=sad&count=9
    ->
    {"status":"FAILED","comment":"handle: Field should contain between 3 and 24 characters, inclusive;from: Field should contain integer value"}

    https://codeforces.com/api/contest.standings?participantTypes=;&contestId=566i ->
    {"status":"FAILED","comment":"contestId: Поле должно содержать длинное целое число;participantTypes: Unknown participant type: \u0027;\u0027"}

     */

    /*
    auth
    "contestId: You have to be authenticated to use this method"
     */

    private fun isCountFieldIncorrect(): Boolean = isIntFieldIncorrect(name = "count")
    private fun isFromFieldIncorrect(): Boolean = isIntFieldIncorrect(name = "from")

    // TODO: Field should contain long integer value
    // TODO: Field should be no more than 100
    private fun isIntFieldIncorrect(name: String): Boolean {
        if (comment.isField(name, "Field should contain integer value")) return true
        if (comment.isField(name, "Field should be at least 1")) return true
        if (comment.isField(name, "Field should be no more than 1000000000")) return true
        return false
    }

    private fun isContestManagerAreNotAllowed(): Boolean {
        if (comment.isField("asManager", "Only contest managers can use \"asManager\" option")) return true
        return false
    }
}

internal fun CodeforcesAPIErrorResponse.toApiException(): CodeforcesApiException {
    // "participantTypes: Unknown participant type: 'AAAAAA'"


    comment.ifIsFieldMsg("handle") { msg ->
        msg.ifSurrounded("User with handle ", " not found") {
            return CodeforcesApiHandleNotFoundException(comment, handle = it)
        }
        when (msg) {
            "You are not allowed to read that blog" -> return CodeforcesApiBlogReadNotAllowedException(comment)
            // "Field should contain between 3 and 24 characters, inclusive", "Поле должно содержать от 3 до 24 символов, включительно"
            // "Field should contain only Latin letters, digits, underscore or dash characters", "Поле должно содержать только латинские буквы, цифры, символы подчёркивание или дефис"
        }
    }

    comment.ifIsFieldMsg("handles") { msg ->
        msg.ifSurrounded("User with handle ", " not found") {
            return CodeforcesApiHandleNotFoundException(comment, handle = it)
        }
        when (msg) {
            // "Field should not be empty", "Поле должно быть не пусто" ->
        }
    }

    comment.ifIsFieldMsg("contestId") { msg ->
        msg.ifIntSurrounded("Contest with id ", " not found") {
            return CodeforcesApiContestNotFoundException(comment, contestId = it)
        }
        msg.ifIntSurrounded("Contest with id ", " has not started") {
            return CodeforcesApiContestNotStartedException(comment, contestId = it)
        }
        when (msg) {
            "Rating changes are unavailable for this contest" -> return CodeforcesApiContestRatingChangesUnavailableException(comment)
            // "Rating changes are unavailable, because the contest isn't finished yet"
        }
    }

    comment.ifIsFieldMsg("blogEntryId") { msg ->
        msg.ifIntSurrounded("Blog entry with id ", " not found") {
            return CodeforcesApiBlogEntryNotFoundException(comment, blogEntryId = it)
        }
    }

    when (comment) {
        "Call limit exceeded" -> return CodeforcesApiCallLimitExceededException(comment)
        // "Internal Server Error" -> status 500
    }

    comment.ifIntSurrounded("Blog entry with id ", " not found") {
        return CodeforcesApiBlogEntryNotFoundException(comment, blogEntryId = it)
    }

    return CodeforcesApiUnspecifiedException(comment)
}

private inline fun String.ifIsFieldMsg(
    name: String,
    block: (String) -> Unit
) {
    // if (startsWith("$name: ")) block(substring(startIndex = name.length + 2))
    val k = name.length
    if (length >= k + 2 && startsWith(name) && get(k) == ':' && get(k + 1) == ' ') {
        block(substring(startIndex = k + 2))
    }
}

private fun String.isField(
    name: String,
    description: String
): Boolean {
//    return this == "name: $description"
    if (length != name.length + 2 + description.length) return false
    if (!startsWith(name)) return false
    if (get(name.length) != ':') return false
    if (get(name.length + 1) != ' ') return false
    if (!endsWith(description)) return false
    return true
}

private inline fun String.ifSurrounded(prefix: String, suffix: String, block: (String) -> Unit) {
    // prefix.length + suffix.length <= length can overflow :)
    if (
        suffix.length <= length &&
        prefix.length <= length - suffix.length &&
        startsWith(prefix) &&
        endsWith(suffix)
    ) {
        block(substring(startIndex = prefix.length, endIndex = length - suffix.length))
    }
}

private inline fun String.ifIntSurrounded(prefix: String, suffix: String, block: (Int) -> Unit) {
    ifSurrounded(prefix = prefix, suffix = suffix) {
        it.toIntOrNull()?.let(block)
    }
}

private inline fun String.ifLongSurrounded(prefix: String, suffix: String, block: (Long) -> Unit) {
    ifSurrounded(prefix = prefix, suffix = suffix) {
        it.toLongOrNull()?.let(block)
    }
}