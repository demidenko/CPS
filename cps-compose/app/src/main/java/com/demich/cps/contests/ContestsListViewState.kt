package com.demich.cps.contests

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.database.ContestPlatform
import com.demich.cps.utils.SafetyLevel
import com.demich.kotlin_stdlib_boost.mapToSet
import com.demich.kotlin_stdlib_boost.minOfNotNull
import com.sebaslogen.resaca.rememberScoped
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

typealias ContestCompositeId = Pair<ContestPlatform, String>

val Contest.compositeId: ContestCompositeId
    get() = platform to id


val Contest.isVirtual: Boolean
    get() = duration < eventDuration

private fun Contest.isOnSamePlatform(other: Contest): Boolean =
    platform == other.platform && (platform != unknown || host == other.host)

private fun Contest.isParallelTo(other: Contest): Boolean =
    isOnSamePlatform(other) &&
    startTime == other.startTime &&
    endTime == other.endTime

@Composable
fun rememberContestsListViewState(): ContestsListViewState {
    val contestsPageState = rememberSaveable {
        mutableStateOf(ContestsListViewState.ContestsPage.RunningOrUpcoming)
    }

    return rememberScoped(key = contestsPageState) {
        ContestsListViewState(
            contestsPageState = contestsPageState,
            noCollisionMinDuration = 1.hours
        )
    }
}

@Stable
class ContestsListViewState(
    contestsPageState: MutableState<ContestsPage>,
    private val noCollisionMinDuration: Duration?
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
        notFinishedIds = contests.runningOrUpcoming.mapToSet { it.compositeId }
    }

    fun isExpanded(contest: Contest): Boolean =
        contest.compositeId in expandedContests

    private fun Contest.isFinished() = compositeId !in notFinishedIds

    fun collisionLevel(contest: Contest): SafetyLevel {
        if (noCollisionMinDuration == null) return SAFE
        val duration = expandedContests.values.minOfNotNull {
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
        if (duration < noCollisionMinDuration) return WARNING
        return SAFE
    }

    var contestsPage by contestsPageState

    enum class ContestsPage {
        Finished, RunningOrUpcoming
    }
}

private inline fun <K, V> MutableState<Map<K, V>>.edit(block: MutableMap<K, V>.() -> Unit) {
    value = value.toMutableMap().apply(block)
}