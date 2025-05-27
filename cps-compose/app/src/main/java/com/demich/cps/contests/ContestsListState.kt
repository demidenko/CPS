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
import com.demich.cps.utils.collectAsState
import com.demich.kotlin_stdlib_boost.minOfNotNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

typealias ContestCompositeId = Pair<Contest.Platform, String>

val Contest.compositeId: ContestCompositeId
    get() = platform to id


val Contest.isVirtual: Boolean
    get() = duration < eventDuration

private fun isParallel(c1: Contest, c2: Contest): Boolean =
    c1.platform == c2.platform && c1.startTime == c2.startTime && c1.endTime == c2.endTime

@Composable
internal fun rememberContestsListState(): ContestsListState {
    val contestsViewModel = contestsViewModel()

    val expandedContestsState = collectAsState { contestsViewModel.flowOfExpandedContests() }

    val contestsPageState = rememberSaveable {
        mutableStateOf(ContestsListState.ContestsPage.RunningOrFuture)
    }

    return remember(contestsViewModel, expandedContestsState) {
        ContestsListState(
            contestsViewModel = contestsViewModel,
            expandedContestsState = expandedContestsState,
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
            else put(key = id, value = contest)
        }
    }

    fun applyContests(contests: List<Contest>) {
        editIds {
            val prev = this.keys.toSet()
            clear()
            contests.forEach { contest ->
                val id = contest.compositeId
                if (id in prev) put(key = id, value = contest)
            }
        }
    }
}

@Stable
class ContestsListState(
    contestsViewModel: ContestsViewModel,
    expandedContestsState: State<Map<ContestCompositeId, Contest>>,
    contestsPageState: MutableState<ContestsPage>
): ContestsIdsHolder by contestsViewModel {
    private val expandedContests by expandedContestsState

    fun isExpanded(contest: Contest): Boolean =
        contest.compositeId in expandedContests

    private val safeMinDuration: Duration = 1.hours
    fun collisionType(contest: Contest): DangerType {
        val distance = expandedContests.values.minOfNotNull {
            val l = it.startTime
            val r = it.endTime
            when {
                isParallel(it, contest) -> null
                it.isVirtual -> null
                l >= contest.endTime -> l - contest.endTime
                r <= contest.startTime -> contest.startTime - r
                else -> return DangerType.DANGER
            }
        } ?: Duration.INFINITE
        if (distance < safeMinDuration) return DangerType.WARNING
        return DangerType.SAFE
    }

    var contestsPage by contestsPageState

    enum class ContestsPage {
        Finished, RunningOrFuture
    }
}
