package com.demich.cps.contests

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.database.ContestPlatform
import com.demich.cps.contests.database.contestsRepository
import com.demich.cps.contests.monitors.CodeforcesMonitorDataStore
import com.demich.cps.contests.monitors.flowOfContestId
import com.demich.cps.utils.context
import com.demich.cps.utils.firstBlocking
import com.demich.cps.utils.flowOfTruncatedCurrentTime
import com.demich.cps.utils.truncateBySeconds
import com.demich.kotlin_stdlib_boost.isSortedWith
import com.demich.kotlin_stdlib_boost.minOfNotNull
import com.demich.kotlin_stdlib_boost.partitionIndex
import com.sebaslogen.resaca.rememberScoped
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.time.Clock
import kotlin.time.Instant


// FINISHED | RUNNING | UPCOMING
data class SortedContests(
    val contests: List<Contest>,
    private val firstRunningOrUpcoming: Int,
    // TODO: firstUpcoming to get contest with phase
) {
    val finished: List<Contest> =
        contests.subList(fromIndex = 0, toIndex = firstRunningOrUpcoming)

    val runningOrUpcoming: List<Contest> =
        contests.subList(fromIndex = firstRunningOrUpcoming, toIndex = contests.size)
}

private interface ContestsSorter {
    val contests: SortedContests
    fun apply(contests: List<Contest>, currentTime: Instant): Boolean
}

private fun List<Contest>.firstRunningOrUpcoming(currentTime: Instant) =
    partitionIndex { it.phaseAt(currentTime) == FINISHED }

private class ContestsBruteSorter: ContestsSorter {
    override var contests: SortedContests = SortedContests(emptyList(), 0)
        private set

    override fun apply(contests: List<Contest>, currentTime: Instant): Boolean {
        val sorted = contests.sortedWith(Contest.comparatorAt(currentTime))
        this.contests = SortedContests(
            contests = sorted,
            firstRunningOrUpcoming = sorted.firstRunningOrUpcoming(currentTime)
        )
        return true
    }
}

private class ContestsSmartSorter: ContestsSorter {
    private class SortedData(contests: List<Contest>, time: Instant) {
        val sorted: List<Contest> =
            contests.let {
                val comparator = Contest.comparatorAt(time)
                if (it.isSortedWith(comparator)) it
                else it.sortedWith(comparator)
            }

        val sortedAt: Instant = time

        val nextReorderTime: Instant =
            sorted.minOfNotNull {
                when {
                    sortedAt < it.startTime -> it.startTime
                    sortedAt < it.endTime -> it.endTime
                    else -> null
                }
            } ?: Instant.DISTANT_FUTURE

        val firstRunningOrUpcoming: Int = sorted.firstRunningOrUpcoming(sortedAt)
    }

    private var last: List<Contest> = emptyList()
    private var sortedLast = SortedData(last, Instant.DISTANT_PAST)

    override val contests: SortedContests
        get() = SortedContests(
            contests = sortedLast.sorted,
            firstRunningOrUpcoming = sortedLast.firstRunningOrUpcoming
        )

    override fun apply(contests: List<Contest>, currentTime: Instant): Boolean {
        with(sortedLast) {
            if (last != contests) {
                last = contests
                sortedLast = SortedData(contests, currentTime)
                return true
            }
            if (currentTime >= nextReorderTime || currentTime < sortedAt) {
                sortedLast = SortedData(sorted, currentTime)
                return true
            }
        }
        return false
    }
}


@Composable
internal fun produceSortedContestsWithTime(
    clock: Clock
): Pair<State<SortedContests>, State<Instant>> {
    val context = context

    val init = rememberScoped {
        val sorter = ContestsSmartSorter()
        val initContests = flowOfContests(context).firstBlocking()
        val initTime = clock.now().truncateBySeconds(1)
        sorter.apply(initContests, initTime)
        val contestsState = mutableStateOf(sorter.contests)
        val currentTimeState = mutableStateOf(initTime)
        Pair(Pair(contestsState, currentTimeState), sorter)
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner, init) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            val (states, sorter: ContestsSorter) = init
            val (contestsState, currentTimeState) = states
            flowOfContests(context).combine(clock.flowOfTruncatedCurrentTime(1)) { contests, currentTime ->
                if (sorter.apply(contests, currentTime)) {
                    contestsState.value = sorter.contests
                }
                currentTimeState.value = currentTime
            }.collect()
        }
    }

    return init.first
}

private fun flowOfIgnoredOrMonitored(context: Context): Flow<Set<ContestCompositeId>> =
    combine(
        flow = ContestsInfoDataStore(context).ignoredContests.asFlow(),
        flow2 = CodeforcesMonitorDataStore(context).flowOfContestId()
    ) { ignored, monitorContestId ->
        buildSet {
            addAll(ignored)
            monitorContestId?.let { add(ContestPlatform.codeforces to it.toString()) }
        }
    }

private fun flowOfContests(context: Context): Flow<List<Contest>> =
    context.contestsRepository.flowOfContests()
        .distinctUntilChanged()
        .combine(flowOfIgnoredOrMonitored(context)) { list, ignored ->
            if (ignored.isEmpty()) list
            else list.filter { contest -> contest.compositeId !in ignored }
        }