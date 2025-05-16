package com.demich.cps.contests

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.database.contestsListDao
import com.demich.cps.contests.monitors.CodeforcesMonitorDataStore
import com.demich.cps.contests.monitors.flowOfContestId
import com.demich.cps.utils.context
import com.demich.cps.utils.truncateBy
import com.demich.cps.utils.flowOfCurrentTimeEachSecond
import com.demich.cps.utils.getCurrentTime
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
    fun sublist(page: ContestsListState.ContestsPage): List<Contest> =
        when (page) {
            ContestsListState.ContestsPage.Finished -> finished
            ContestsListState.ContestsPage.RunningOrFuture -> runningOrFuture
        }

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
        val sorted = contests.sortedWith(Contest.getComparator(currentTime))
        this.contests = SortedContests(
            contests = sorted,
            firstFinished = sorted.firstFinished(currentTime)
        )
        return true
    }
}

private class ContestsSmartSorter: ContestsSorter {
    private var last: List<Contest> = emptyList()
    private var sortedLast: List<Contest> = emptyList()
    private var firstFinished: Int = 0
    private var sortedAt: Instant = Instant.DISTANT_PAST
    private var nextReorderTime: Instant = Instant.DISTANT_FUTURE

    override val contests: SortedContests
        get() = SortedContests(
            contests = sortedLast,
            firstFinished = firstFinished
        )

    private fun saveToSorted(contests: List<Contest>, currentTime: Instant) {
        sortedLast = contests.sortedWith(Contest.getComparator(currentTime))
        sortedAt = currentTime
        nextReorderTime = contests.minOfNotNull {
            when {
                currentTime < it.startTime -> it.startTime
                currentTime < it.endTime -> it.endTime
                else -> null
            }
        } ?: Instant.DISTANT_FUTURE
    }

    private fun updateFirstFinished(currentTime: Instant) {
        firstFinished = sortedLast.firstFinished(currentTime)
    }

    override fun apply(contests: List<Contest>, currentTime: Instant): Boolean {
        if (last != contests || currentTime < sortedAt) {
            last = contests
            saveToSorted(contests, currentTime)
            updateFirstFinished(currentTime)
            return true
        }
        if (currentTime >= nextReorderTime) {
            if (!sortedLast.isSortedWith(Contest.getComparator(currentTime))) {
                saveToSorted(sortedLast, currentTime)
            }
            updateFirstFinished(currentTime)
            return true
        }
        return false
    }
}


@Composable
internal fun produceSortedContestsWithTime(

): Pair<State<SortedContests>, State<Instant>> {
    val context = context

    val states = remember {
        val initContests = runBlocking { flowOfContests(context).first() }
        val currentTime = getCurrentTime().truncateBy(1.seconds)
        val sorter = ContestsBruteSorter()
        sorter.apply(initContests, currentTime)
        val contestsState = mutableStateOf(sorter.contests)
        val currentTimeState = mutableStateOf(currentTime)
        Pair(contestsState, currentTimeState)
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner, states) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            val (contestsState, currentTimeState) = states
            val sorter: ContestsSorter = ContestsSmartSorter()
            flowOfContests(context).combine(flowOfCurrentTimeEachSecond()) { contests, currentTime ->
                if (sorter.apply(contests, currentTime)) {
                    contestsState.value = sorter.contests
                }
                currentTimeState.value = currentTime
            }.collect()
        }
    }

    return states
}