package com.demich.cps.ui.filter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.demich.cps.utils.filterByTokensAsSubsequence


@Composable
fun rememberFilterState(): FilterState =
    rememberSaveable(saver = saver()) {
        FilterState(
            string = "",
            enabled = false
        )
    }

@Stable //TODO: not enough, compose ignores
class FilterState(
    string: String,
    enabled: Boolean
) {
    var string by mutableStateOf(string)

    var enabled by mutableStateOf(enabled)

    private val availableState = mutableStateOf(false)
    var available: Boolean
        get() = availableState.value
        set(value) {
            availableState.value = value
            if (!value) enabled = false
        }
}


private fun saver() =
    listSaver<FilterState, String>(
        save = {
            listOf(it.string, it.enabled.toString())
        },
        restore = {
            FilterState(
                string = it[0],
                enabled = it[1].toBooleanStrict()
            )
        }
    )

inline fun <T> List<T>.filterByTokensAsSubsequence(
    filterState: FilterState,
    toCheck: T.() -> Sequence<String>
): List<T> {
    if (!filterState.enabled) return this
    return filterByTokensAsSubsequence(filter = filterState.string, toCheck = toCheck)
}