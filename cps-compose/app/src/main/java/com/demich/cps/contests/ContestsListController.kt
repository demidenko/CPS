package com.demich.cps.contests

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.demich.cps.contests.database.Contest
import com.demich.cps.utils.DangerType
import com.demich.cps.utils.rememberCollect
import com.demich.kotlin_stdlib_boost.minOfNotNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

typealias ContestCompositeId = Pair<Contest.Platform, String>

@Composable
internal fun rememberContestsListController(): ContestsListController {
    val contestsViewModel = contestsViewModel()

    val expandedIdsState = rememberCollect { contestsViewModel.flowOfExpandedContests() }

    val contestsPageState = rememberSaveable {
        mutableStateOf(ContestsListController.ContestsPage.RunningOrFuture)
    }

    return remember(contestsViewModel, expandedIdsState) {
        ContestsListController(
            contestsViewModel = contestsViewModel,
            expandedIdsState = expandedIdsState,
            contestsPageState = contestsPageState
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
class ContestsListController(
    contestsViewModel: ContestsViewModel,
    expandedIdsState: State<Map<ContestCompositeId, Contest>>,
    contestsPageState: MutableState<ContestsPage>
): ContestsIdsHolder by contestsViewModel {
    private val expandedIds by expandedIdsState

    fun isExpanded(contest: Contest): Boolean =
        contest.compositeId in expandedIds

    private val safeMinDuration: Duration = 1.hours
    fun collisionType(contest: Contest): DangerType {
        val distance = expandedIds.entries.minOfNotNull(default = Duration.INFINITE) { (id, it) ->
            val l = it.startTime
            val r = it.endTime
            when {
                id == contest.compositeId -> null
                l >= contest.endTime -> l - contest.endTime
                r <= contest.startTime -> contest.startTime - r
                else -> return DangerType.DANGER
            }
        }
        if (distance < safeMinDuration) return DangerType.WARNING
        return DangerType.SAFE
    }

    var contestsPage by contestsPageState

    enum class ContestsPage {
        Finished, RunningOrFuture
    }
}
