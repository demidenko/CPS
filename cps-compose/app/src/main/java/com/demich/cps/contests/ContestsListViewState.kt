package com.demich.cps.contests

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.demich.cps.contests.database.Contest
import com.demich.cps.utils.SafetyLevel
import com.demich.kotlin_stdlib_boost.mapToSet
import com.demich.kotlin_stdlib_boost.minOfNotNull
import com.sebaslogen.resaca.rememberScoped
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

typealias ContestCompositeId = Pair<Contest.Platform, String>

val Contest.compositeId: ContestCompositeId
    get() = platform to id


val Contest.isVirtual: Boolean
    get() = duration < eventDuration

private fun Contest.isParallelTo(c2: Contest): Boolean =
    platform == c2.platform &&
    startTime == c2.startTime &&
    endTime == c2.endTime

@Composable
fun rememberContestsListViewState(): ContestsListViewState {
    val contestsPageState = rememberSaveable {
        mutableStateOf(ContestsListViewState.ContestsPage.RunningOrFuture)
    }

    return rememberScoped(key = contestsPageState) {
        ContestsListViewState(contestsPageState = contestsPageState)
    }
}

@Stable
class ContestsListViewState(
    contestsPageState: MutableState<ContestsPage>
) {
    private val expandedContestsState = mutableStateOf<Map<ContestCompositeId, Contest>>(emptyMap())
    private val expandedContests by expandedContestsState
    private var notFinishedIds by mutableStateOf<Set<ContestCompositeId>>(emptySet())

    fun toggleExpanded(contest: Contest) {
        expandedContestsState.edit {
            val id = contest.compositeId
            if (id in this) remove(id)
            else put(key = id, value = contest)
        }
    }

    fun syncExpanded(contests: SortedContests) {
        val prev = expandedContests
        expandedContestsState.value = buildMap {
            contests.contests.forEach { contest ->
                val id = contest.compositeId
                if (id in prev) put(key = id, value = contest)
            }
        }
        notFinishedIds = contests.runningOrFuture.mapToSet { it.compositeId }
    }

    fun isExpanded(contest: Contest): Boolean =
        contest.compositeId in expandedContests

    private fun Contest.isFinished() = compositeId !in notFinishedIds

    private val safeMinDuration: Duration get() = 1.hours
    fun collisionLevel(contest: Contest): SafetyLevel {
        val distance = expandedContests.values.minOfNotNull {
            val l = it.startTime
            val r = it.endTime
            when {
                it.isParallelTo(contest) -> null
                it.isVirtual -> null
                it.isFinished() -> null
                l >= contest.endTime -> l - contest.endTime
                r <= contest.startTime -> contest.startTime - r
                else -> return DANGER
            }
        } ?: Duration.INFINITE
        if (distance < safeMinDuration) return WARNING
        return SAFE
    }

    var contestsPage by contestsPageState

    enum class ContestsPage {
        Finished, RunningOrFuture
    }
}

private inline fun <K, V> MutableState<Map<K, V>>.edit(block: MutableMap<K, V>.() -> Unit) {
    value = value.toMutableMap().apply(block)
}