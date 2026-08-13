package com.demich.cps.ui.filter

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.demich.cps.utils.filterByTokensAsSubsequence


@Composable
fun rememberFilterState(): FilterState {
    val textFieldState = rememberSaveable(saver = TextFieldState.Saver) { TextFieldState() }
    val enabledState = rememberSaveable { mutableStateOf(false) }
    return remember {
        FilterState(
            textFieldState = textFieldState,
            enabledState = enabledState
        )
    }
}

@Stable //TODO: not enough, compose ignores
class FilterState(
    val textFieldState: TextFieldState,
    enabledState: MutableState<Boolean>
) {
    val string: String
        get() = textFieldState.text.toString()

    var enabled by enabledState

    private val availableState = mutableStateOf(false)
    var available: Boolean
        get() = availableState.value
        set(value) {
            availableState.value = value
            if (!value) enabled = false
        }
}

inline fun <T> List<T>.filterByTokensAsSubsequence(
    filterState: FilterState,
    toCheck: T.() -> Sequence<String>
): List<T> {
    if (!filterState.enabled) return this
    return filterByTokensAsSubsequence(filter = filterState.string, toCheck = toCheck)
}