package com.demich.cps.ui.filter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue


@Composable
fun rememberFilterState() =
    rememberSaveable(saver = saver()) {
        FilterState(
            filter = "",
            enabled = false,
            available = false
        )
    }

@Stable //TODO: not enough, compose ignores
class FilterState(
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
}


private fun saver() =
    listSaver<FilterState, String>(
        save = {
            listOf(it.filter, it.enabled.toString(), it.available.toString())
        },
        restore = {
            FilterState(
                filter = it[0],
                enabled = it[1].toBooleanStrict(),
                available = it[2].toBooleanStrict()
            )
        }
    )
