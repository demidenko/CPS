package com.demich.cps.contests

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.demich.cps.utils.containsTokensAsSubsequence


@Composable
fun rememberContestsFilterController(): ContestsFilterController {
    val filterState = rememberSaveable { mutableStateOf("") }
    val enabledState = rememberSaveable { mutableStateOf(false) }
    val availableState = rememberSaveable { mutableStateOf(false) }
    return remember(enabledState, filterState, availableState) {
        ContestsFilterController(
            filterState = filterState,
            enabledState = enabledState,
            availableState = availableState
        )
    }
}

@Stable
data class ContestsFilterController(
    private val filterState: MutableState<String>,
    private val enabledState: MutableState<Boolean>,
    private val availableState: MutableState<Boolean>
) {

    var filter: String
        get() = filterState.value
        set(value) { filterState.value = value }

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

}