package com.demich.cps.contests

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import com.demich.cps.contests.database.Contest
import com.demich.cps.utils.containsTokensAsSubsequence


@Composable
fun rememberContestsFilterController() =
    rememberSaveable(saver = ContestsFilterController.saver) {
        ContestsFilterController(
            filter = "",
            enabled = false,
            available = false
        )
    }

@Stable //TODO: not enough, compose ignores
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
        val tokens = filter.trim().also {
            if (it.isEmpty()) return contests
        }.split(whiteSpaceRegex)
        return contests.filter { checkContest(it, tokens) }
    }

    companion object {
        val saver get() = listSaver<ContestsFilterController, String>(
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

        private val whiteSpaceRegex = "\\s+".toRegex()
    }
}

private fun List<String>.check(string: String) =
    string.containsTokensAsSubsequence(tokens = this, ignoreCase = true)

private fun checkContest(contest: Contest, tokens: List<String>): Boolean {
    with(contest) {
        if (tokens.check(title)) return true
        if (platform != Contest.Platform.unknown && tokens.check(platform.name)) return true
        host?.let { if (tokens.check(it)) return true }
    }
    return false
}