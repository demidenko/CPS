package com.demich.cps.contests

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.demich.cps.utils.containsTokensAsSubsequence


@Composable
fun rememberContestsFilterController(): ContestsFilterController {
    val enabledState = rememberSaveable { mutableStateOf(false) }
    val filterState = rememberSaveable { mutableStateOf("") }
    return remember(enabledState, filterState) {
        ContestsFilterController(
            enabledState = enabledState,
            filterState = filterState
        )
    }
}

@Stable
data class ContestsFilterController(
    private val enabledState: MutableState<Boolean>,
    private val filterState: MutableState<String>
) {

    fun checkContest(contest: Contest): Boolean {
        if(contest.title.containsTokensAsSubsequence(str = filter, ignoreCase = true)) return true
        if(contest.platform.name.containsTokensAsSubsequence(str = filter, ignoreCase = true)) return true
        return false
    }

    var enabled: Boolean
        get() = enabledState.value
        set(value) { enabledState.value = value }


    var filter: String
        get() = filterState.value
        set(value) { filterState.value = value }

}