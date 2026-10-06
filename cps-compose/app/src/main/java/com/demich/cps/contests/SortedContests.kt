package com.demich.cps.contests

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.database.contestsRepository
import com.demich.cps.contests.monitors.CodeforcesMonitorDataStore
import com.demich.cps.contests.monitors.flowOfContestId
import com.demich.cps.utils.context
import com.demich.cps.utils.firstBlocking
import com.demich.cps.utils.flowOfTruncatedCurrentTime
import com.demich.cps.utils.truncateBySeconds
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
    private val firstRunningOrUpcoming: Int
    // TODO: firstUpcoming to get contest with phase
) {
    constructor(
        contests: List<Contest>,
        sortedAt: Instant
    ): this(
        contests = contests,
        firstRunningOrUpcoming = contests.partitionIndex { it.phaseAt(sortedAt) == FINISHED }
    )

    val finished: List<Contest> =
        contests.subList(fromIndex = 0, toIndex = firstRunningOrUpcoming)

    val runningOrUpcoming: List<Contest> =
        contests.subList(fromIndex = firstRunningOrUpcoming, toIndex = contests.size)
}

private fun List<Contest>.nextReorderTime(sortedAt: Instant): Instant =
    minOfNotNull {
        when {
            sortedAt < it.startTime -> it.startTime
            sortedAt < it.endTime -> it.endTime
            else -> null
        }
    } ?: Instant.DISTANT_FUTURE

private fun List<Contest>.sortedOrThisAt(at: Instant): List<Contest> {
    val comparator = Contest.comparatorAt(at)
    return if (isSortedWith(comparator)) this
    else sortedWith(comparator)
}

private interface ContestsSorter {
    val contests: SortedContests
    fun apply(contests: List<Contest>, time: Instant): Boolean
}

private class ContestsSmartSorter: ContestsSorter {
    private class SortedData private constructor(
        val source: List<Contest>,
        val sortedAt: Instant,
        val sorted: List<Contest>
    ) {
        constructor(
            source: List<Contest>,
            sortedAt: Instant
        ): this(
            source = source,
            sortedAt = sortedAt,
            sorted = source.sortedOrThisAt(sortedAt)
        )

        private val nextReorderTime: Instant = sorted.nextReorderTime(sortedAt)

        fun sameOrder(time: Instant): Boolean =
            time >= sortedAt && time < nextReorderTime

        val result = SortedContests(
            contests = sorted,
            sortedAt = sortedAt
        )

        fun sort(at: Instant) =
            // !sameOrder
            SortedData(
                source = source,
                sortedAt = at,
                sorted = sorted.sortedOrThisAt(at)
            )
    }
    private var last = SortedData(source = emptyList(), sortedAt = Instant.DISTANT_PAST)

    override val contests: SortedContests
        get() = last.result

    override fun apply(contests: List<Contest>, time: Instant): Boolean {
        with(last) {
            if (source != contests) {
                last = SortedData(source = contests, sortedAt = time)
                return true
            }
            if (!sameOrder(time)) {
                last = sort(at = time)
                return true
            }
            return false
        }
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
        val initTime = clock.now().truncateBySeconds()
        sorter.apply(initContests, initTime)
        val contestsState = mutableStateOf(sorter.contests)
        val currentTimeState = mutableStateOf(initTime)
        Pair(Pair(contestsState, currentTimeState), sorter)
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner, init) {
        lifecycleOwner.repeatOnLifecycle(state = STARTED) {
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
        if (monitorContestId == null) ignored
        else ignored + Pair(codeforces, monitorContestId.toString())
    }

private fun flowOfContests(context: Context): Flow<List<Contest>> =
    context.contestsRepository.flowOfContests()
        .distinctUntilChanged()
        .combine(flowOfIgnoredOrMonitored(context)) { list, ignored ->
            if (ignored.isEmpty()) list
            else list.filter { contest -> contest.compositeId !in ignored }
        }