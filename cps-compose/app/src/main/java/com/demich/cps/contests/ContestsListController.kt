package com.demich.cps.contests

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.demich.cps.contests.database.Contest
import com.demich.cps.utils.rememberCollect

typealias ContestCompositeId = Pair<Contest.Platform, String>

@Composable
internal fun rememberContestsListController(): ContestsListController {
    val contestsViewModel = contestsViewModel()

    val expandedIdsState = rememberCollect { contestsViewModel.flowOfExpandedContests() }

    return remember(expandedIdsState, contestsViewModel) {
        ContestsListController(
            expandedIdsState = expandedIdsState,
            contestsViewModel = contestsViewModel
        )
    }
}

internal interface ContestsIdsHolder {
    fun editIds(block: MutableMap<ContestCompositeId, Contest>.() -> Unit)

    fun toggleExpanded(contest: Contest) {
        editIds {
            val id = contest.compositeId
            if (id in this) remove(id)
            else put(id, contest)
        }
    }

    fun applyContests(contests: List<Contest>) {
        val new = contests.associateBy { it.compositeId }
        editIds {
            keys.removeAll { it !in new }
            replaceAll { id, _ -> new.getValue(id) }
        }
    }
}

@Stable
internal class ContestsListController(
    expandedIdsState: State<Map<ContestCompositeId, Contest>>,
    contestsViewModel: ContestsViewModel
): ContestsIdsHolder by contestsViewModel {
    private val expandedIds by expandedIdsState

    fun isExpanded(contest: Contest): Boolean =
        contest.compositeId in expandedIds
}
