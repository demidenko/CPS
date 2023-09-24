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
import com.demich.cps.utils.floorBy
import com.demich.cps.utils.flowOfCurrentTimeEachSecond
import com.demich.cps.utils.getCurrentTime
import com.demich.cps.utils.isSortedWith
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.seconds

internal data class SortedContests(
    val contests: List<Contest>,
    val currentTime: Instant
)

private fun flowOfIgnoredOrMonitored(context: Context): Flow<Set<Pair<Contest.Platform, String>>> =
    combine(
        flow = ContestsInfoDataStore(context).ignoredContests.flow,
        flow2 = CodeforcesMonitorDataStore(context).flowOfContestId()
    ) { ignored, monitorContestId ->
        buildSet {
            addAll(ignored.keys)
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

private interface ContestsSorter {
    val contests: List<Contest>
    fun apply(contests: List<Contest>, currentTime: Instant)
}

/*private class ContestsStupidSorter: ContestsSorter {
    override var contests: List<Contest> = emptyList()
        private set

    override fun apply(contests: List<Contest>, currentTime: Instant) {
        this.contests = contests.sortedWith(Contest.getComparator(currentTime))
    }
}*/

private class ContestsDefaultSorter: ContestsSorter {
    private var last: List<Contest> = emptyList()
    private var sortedLast: List<Contest> = emptyList()

    override val contests: List<Contest> get() = sortedLast

    override fun apply(contests: List<Contest>, currentTime: Instant) {
        if (last != contests) {
            last = contests
            sortedLast = contests
        }
        val comparator = Contest.getComparator(currentTime)
        if (!sortedLast.isSortedWith(comparator)) {
            sortedLast = sortedLast.sortedWith(comparator)
        }
    }
}

private class ContestsSmartSorter: ContestsSorter {
    private var last: List<Contest> = emptyList()
    private var sortedLast: List<Contest> = emptyList()
    private var sortedAt: Instant = Instant.DISTANT_PAST
    private var nextSortMoment: Instant = Instant.DISTANT_PAST

    override val contests: List<Contest> get() = sortedLast

    override fun apply(contests: List<Contest>, currentTime: Instant) {
        if (last != contests || currentTime < sortedAt) {
            last = contests
            sortedLast = contests.sortedWith(Contest.getComparator(currentTime))
            sortedAt = currentTime
            nextSortMoment = contests.nextSortMoment(currentTime)
        } else {
            if (currentTime >= nextSortMoment) {
                val comparator = Contest.getComparator(currentTime)
                if (!sortedLast.isSortedWith(comparator)) {
                    sortedLast = sortedLast.sortedWith(comparator)
                    sortedAt = currentTime
                    nextSortMoment = contests.nextSortMoment(currentTime)
                }
            }
        }
    }
    companion object {
        private fun List<Contest>.nextSortMoment(currentTime: Instant): Instant =
            minOfOrNull {
                when (it.getPhase(currentTime)) {
                    Contest.Phase.BEFORE -> it.startTime
                    Contest.Phase.RUNNING -> it.endTime
                    Contest.Phase.FINISHED -> Instant.DISTANT_FUTURE
                }
            } ?: Instant.DISTANT_FUTURE
    }
}

internal fun flowOfSortedContestsWithTime(context: Context): Flow<SortedContests> {
    val sorter: ContestsSorter = ContestsSmartSorter()
    return flowOfContests(context).combine(flowOfCurrentTimeEachSecond()) { contests, currentTime ->
        sorter.apply(contests, currentTime)
        SortedContests(sorter.contests, currentTime)
    }
}

@Composable
internal fun produceSortedContestsWithTime(

): Pair<State<List<Contest>>, State<Instant>> {
    val context = context

    val states = remember {
        val contests = runBlocking { flowOfContests(context).first() }
        val currentTime = getCurrentTime().floorBy(1.seconds)
        val sortedContests = contests.sortedWith(Contest.getComparator(currentTime))
        val contestsState = mutableStateOf(sortedContests)
        val currentTimeState = mutableStateOf(currentTime)
        Pair(contestsState, currentTimeState)
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner, states) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            val (contestsState, currentTimeState) = states
            //TODO: optimize emplace (??)
            flowOfSortedContestsWithTime(context)
                .collect {
                    contestsState.value = it.contests
                    currentTimeState.value = it.currentTime
                }
        }
    }

    return states
}