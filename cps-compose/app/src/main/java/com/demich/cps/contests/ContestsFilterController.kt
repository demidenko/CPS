package com.demich.cps.contests

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.demich.cps.contests.database.Contest
import com.demich.cps.utils.filterByTokensAsSubsequence


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

    fun filterContests(contests: List<Contest>): List<Contest> =
        contests.filterByTokensAsSubsequence(filter) {
            sequence {
                yield(title)
                if (platform != Contest.Platform.unknown) yield(platform.name)
                host?.let { yield(it) }
            }
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
    }
}
