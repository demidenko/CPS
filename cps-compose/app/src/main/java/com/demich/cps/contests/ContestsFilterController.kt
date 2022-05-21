package com.demich.cps.contests

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
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

    fun checkContest(contest: Contest): Boolean {
        if(contest.title.containsTokensAsSubsequence(str = filter, ignoreCase = true)) return true
        if(contest.platform.name.containsTokensAsSubsequence(str = filter, ignoreCase = true)) return true
        return false
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