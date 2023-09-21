package com.demich.cps.contests

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.demich.cps.contests.database.Contest
import com.demich.cps.utils.jsonCPS
import com.demich.cps.utils.mapToSet
import com.demich.cps.utils.saver

typealias ContestCompositeId = Pair<Contest.Platform, String>

@Composable
fun rememberContestsListController(): ContestsListController {
    val expandedIdsState = rememberSaveable(stateSaver = jsonCPS.saver()) {
        mutableStateOf(emptyList<ContestCompositeId>())
    }

    return remember(expandedIdsState) {
        ContestsListController(
            expandedIdsState = expandedIdsState
        )
    }
}

@Stable
class ContestsListController(
    expandedIdsState: MutableState<List<ContestCompositeId>>
) {
    private var expandedIds by expandedIdsState

    fun isExpanded(contest: Contest): Boolean =
        contest.compositeId in expandedIds

    fun toggleExpanded(contest: Contest) {
        val id = contest.compositeId
        if (id in expandedIds) expandedIds -= id
        else expandedIds += id
    }

    fun removeUnavailable(contests: List<Contest>) {
        val currentsIds = contests.mapToSet { it.compositeId }
        expandedIds = expandedIds.filter { it in currentsIds }
    }
}