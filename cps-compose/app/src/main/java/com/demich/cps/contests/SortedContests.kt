package com.demich.cps.contests

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.database.contestsListDao
import com.demich.cps.contests.monitors.CodeforcesMonitorDataStore
import com.demich.cps.contests.monitors.flowOfContestId
import com.demich.cps.utils.context
import com.demich.cps.utils.flowOfCurrentTimeEachSecond
import com.demich.cps.utils.getCurrentTime
import com.demich.cps.utils.truncateBy
import com.demich.kotlin_stdlib_boost.isSortedWith
import com.demich.kotlin_stdlib_boost.minOfNotNull
import com.demich.kotlin_stdlib_boost.partitionIndex
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.seconds

private fun flowOfIgnoredOrMonitored(context: Context): Flow<Set<ContestCompositeId>> =
    combine(
        flow = ContestsInfoDataStore(context).ignoredContests.flow,
        flow2 = CodeforcesMonitorDataStore(context).flowOfContestId()
    ) { ignored, monitorContestId ->
        buildSet {
            addAll(ignored)
            monitorContestId?.let { add(Contest.Platform.codeforces to it.toString()) }
        }
    }

private fun flowOfContests(context: Context) =
    context.contestsListDao.flowOfContests()
        .distinctUntilChanged()
        .combine(flowOfIgnoredOrMonitored(context)) { list, ignored ->
            if (ignored.isEmpty()) list
            else list.filter { contest -> contest.compositeId !in ignored }
        }

internal data class SortedContests(
    val contests: List<Contest>,
    private val firstFinished: Int
) {
    val finished: List<Contest> =
        contests.subList(fromIndex = firstFinished, toIndex = contests.size)

    val runningOrFuture: List<Contest> =
        contests.subList(fromIndex = 0, toIndex = firstFinished)
}

private interface ContestsSorter {
    val contests: SortedContests
    fun apply(contests: List<Contest>, currentTime: Instant): Boolean
}

private fun List<Contest>.firstFinished(currentTime: Instant) =
    partitionIndex { it.getPhase(currentTime) != Contest.Phase.FINISHED }

private class ContestsBruteSorter: ContestsSorter {
    override var contests: SortedContests = SortedContests(emptyList(), 0)
        private set

    override fun apply(contests: List<Contest>, currentTime: Instant): Boolean {
        val sorted = contests.sortedWith(Contest.comparatorAt(currentTime))
        this.contests = SortedContests(
            contests = sorted,
            firstFinished = sorted.firstFinished(currentTime)
        )
        return true
    }
}

private class ContestsSmartSorter: ContestsSorter {
    private class SortedList(data: List<Contest>, time: Instant) {
        val list: List<Contest> = data.sortedWith(Contest.comparatorAt(time))

        val sortedAt: Instant = time

        val nextReorderTime: Instant = list.minOfNotNull {
            when {
                sortedAt < it.startTime -> it.startTime
                sortedAt < it.endTime -> it.endTime
                else -> null
            }
        } ?: Instant.DISTANT_FUTURE
    }

    private var last: List<Contest> = emptyList()
    private var sortedLast = SortedList(last, Instant.DISTANT_PAST)
    private var firstFinished: Int = 0

    override val contests: SortedContests
        get() = SortedContests(
            contests = sortedLast.list,
            firstFinished = firstFinished
        )

    private fun updateFirstFinished(currentTime: Instant) {
        firstFinished = sortedLast.list.firstFinished(currentTime)
    }

    override fun apply(contests: List<Contest>, currentTime: Instant): Boolean {
        if (last != contests || currentTime < sortedLast.sortedAt) {
            last = contests
            sortedLast = SortedList(contests, currentTime)
            updateFirstFinished(currentTime)
            return true
        }
        with(sortedLast) {
            if (currentTime >= nextReorderTime) {
                if (!list.isSortedWith(Contest.comparatorAt(currentTime))) {
                    sortedLast = SortedList(list, currentTime)
                }
                updateFirstFinished(currentTime)
                return true
            }
        }
        return false
    }
}


@Composable
internal fun produceSortedContestsWithTime(

): Pair<State<SortedContests>, State<Instant>> {
    val context = context

    val init = remember {
        val initContests = runBlocking { flowOfContests(context).first() }
        val currentTime = getCurrentTime().truncateBy(1.seconds)
        val sorter = ContestsSmartSorter()
        sorter.apply(initContests, currentTime)
        val contestsState = mutableStateOf(sorter.contests)
        val currentTimeState = mutableStateOf(currentTime)
        Pair(Pair(contestsState, currentTimeState), sorter)
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner, init) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            val (states, sorter: ContestsSorter) = init
            val (contestsState, currentTimeState) = states
            flowOfContests(context).combine(flowOfCurrentTimeEachSecond()) { contests, currentTime ->
                if (sorter.apply(contests, currentTime)) {
                    contestsState.value = sorter.contests
                }
                currentTimeState.value = currentTime
            }.collect()
        }
    }

    return init.first
}