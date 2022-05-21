package com.demich.cps.contests

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable


@Composable
fun rememberContestsFilterController() =
    rememberSaveable(saver = ContestsFilterController.saver) {
        ContestsFilterController(
            filter = "",
            enabled = false,
            available = false
        )
    }

@Stable
class ContestsFilterController(
    filter: String,
    enabled: Boolean,
    available: Boolean
) {
    var filter by mutableStateOf(filter)
    private val enabledState = mutableStateOf(enabled)
    private val availableState = mutableStateOf(available)

    var enabled: Boolean
        get() = enabledState.value
        set(value) {
            enabledState.value = value
            if (!value) filter = ""
        }

    var available: Boolean
        get() = availableState.value
        set(value) {
            availableState.value = value
            if (!value) enabled = false
        }

    fun filterContests(contests: List<Contest>): List<Contest> {
        val tokens = filter.trim().split(whiteSpaceRegex)
        return contests.filter { checkContest(it, tokens) }
    }

    companion object {
        val saver = listSaver<ContestsFilterController, String>(
            save = {
                listOf(it.filter, it.enabled.toString(), it.available.toString())
            },
            restore = {
                ContestsFilterController(
                    filter = it[0],
                    enabled = it[1].toBooleanStrict(),
                    available = it[2].toBooleanStrict()
                )
            }
        )
    }
}

private val whiteSpaceRegex = "\\s+".toRegex()

//can't resist to note that it be solved in O(nlogn) by suffix array + segment tree
private fun String.containsTokensAsSubsequence(tokens: List<String>, ignoreCase: Boolean = false): Boolean {
    var i = 0
    for (token in tokens) {
        val pos = indexOf(string = token, ignoreCase = ignoreCase, startIndex = i)
        if (pos == -1) return false
        i = pos + token.length
    }
    return true
}

private fun checkString(string: String, tokens: List<String>) =
    string.containsTokensAsSubsequence(tokens = tokens, ignoreCase = true)

private fun checkContest(contest: Contest, tokens: List<String>): Boolean {
    if(checkString(contest.title, tokens)) return true
    if(contest.platform != Contest.Platform.unknown && checkString(contest.platform.name, tokens)) return true
    return false
}