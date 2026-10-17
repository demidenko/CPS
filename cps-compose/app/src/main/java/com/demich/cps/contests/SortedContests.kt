package com.demich.cps.contests

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.demich.cps.contests.database.Contest
import com.demich.cps.utils.flowOfTruncatedCurrentTime
import com.demich.cps.utils.truncateBySeconds
import com.demich.kotlin_stdlib_boost.minOfNotNull
import com.demich.kotlin_stdlib_boost.partitionIndex
import com.sebaslogen.resaca.rememberScoped
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.adapters.ImmutableListAdapter
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
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

private fun ImmutableList<Contest>.sortedAt(at: Instant): ImmutableList<Contest> {
    val comparator = Contest.comparatorAt(at)
    return if (isSortedWith(comparator)) this
    else ImmutableListAdapter(sortedWith(comparator))
}

private abstract class ContestsSorter {
    protected class SortedData private constructor(
        val source: ImmutableList<Contest>,
        val sortedAt: Instant,
        val sorted: ImmutableList<Contest>
    ) {
        constructor(
            source: ImmutableList<Contest>,
            sortedAt: Instant
        ): this(
            source = source,
            sortedAt = sortedAt,
            sorted = source.sortedAt(sortedAt)
        )

        private val nextReorderTime: Instant = sorted.nextReorderTime(sortedAt)

        val result = SortedContests(
            contests = sorted,
            sortedAt = sortedAt
        )

        fun sort(time: Instant): SortedData =
            if (time >= sortedAt && time < nextReorderTime) {
                this
            } else {
                SortedData(
                    source = source,
                    sortedAt = time,
                    sorted = sorted.sortedAt(time)
                )
            }
    }
    protected abstract var last: SortedData?

    fun update(source: ImmutableList<Contest>, time: Instant) {
        last.let {
            if (it == null || it.source != source) {
                last = SortedData(source = source, sortedAt = time)
            } else {
                last = it.sort(time = time)
            }
        }
    }
}

private class SortedContestsState: ContestsSorter(), State<SortedContests?> {

    override var last: SortedData? by mutableStateOf(null)

    override val value: SortedContests?
        get() = last?.result

}


@Composable
fun produceSortedContestsWithTime(
    flow: Flow<List<Contest>>,
    clock: Clock
): Pair<State<SortedContests?>, State<Instant>> {
    val sorter = rememberScoped { SortedContestsState() }

    val currentTimeState = remember(clock) {
        val initTime = clock.now().truncateBySeconds()
        mutableStateOf(initTime)
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner, flow, sorter, currentTimeState) {
        lifecycleOwner.repeatOnLifecycle(state = STARTED) {
            combine(
                flow = flow.distinctUntilChanged().map { it.toImmutableList() },
                flow2 = clock.flowOfTruncatedCurrentTime(1)
            ) { contests, currentTime ->
                sorter.update(contests, currentTime)
                currentTimeState.value = currentTime
            }.collect()
        }
    }

    return Pair(sorter, currentTimeState)
}
