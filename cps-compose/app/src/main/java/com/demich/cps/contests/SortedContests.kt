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
import com.demich.cps.utils.context
import com.demich.cps.utils.floorBy
import com.demich.cps.utils.flowOfCurrentTimeEachSecond
import com.demich.cps.utils.getCurrentTime
import com.demich.cps.utils.isSortedWith
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
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
        flow2 = CodeforcesMonitorDataStore(context).contestId.flow
    ) { ignored, monitorContestId ->
        ignored.keys.let {
            if (monitorContestId == null) it
            else it.plus(Contest.Platform.codeforces to monitorContestId.toString())
        }
    }

private fun flowOfContests(context: Context) =
    combine(
        flow = context.contestsListDao.flowOfContests().distinctUntilChanged(),
        flow2 = flowOfIgnoredOrMonitored(context)
    ) { list, ignored ->
        if (ignored.isEmpty()) list
        else list.filter { contest -> contest.compositeId !in ignored }
    }

internal fun flowOfSortedContestsWithTime(context: Context): Flow<SortedContests> {
    var last: List<Contest> = emptyList()
    var sortedLast: List<Contest> = emptyList()
    return flowOfContests(context).combine(flowOfCurrentTimeEachSecond()) { contests, currentTime ->
        if (last != contests) {
            last = contests
            sortedLast = contests
        }
        val comparator = Contest.getComparator(currentTime)
        if (!sortedLast.isSortedWith(comparator)) {
            sortedLast = sortedLast.sortedWith(comparator)
        }
        SortedContests(sortedLast, currentTime)
    }
}

@Composable
internal fun produceSortedContestsWithTime(

): Pair<State<List<Contest>>, State<Instant>> {
    val context = context

    val initPair = remember {
        val contests = runBlocking { flowOfContests(context).first() }
        val currentTime = getCurrentTime().floorBy(1.seconds)
        SortedContests(
            contests = contests.sortedWith(Contest.getComparator(currentTime)),
            currentTime = currentTime
        )
    }

    val contestsState = remember(initPair) { mutableStateOf(initPair.contests) }
    val currentTimeState = remember(initPair) { mutableStateOf(initPair.currentTime) }

    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            //TODO: optimize emplace (??)
            flowOfSortedContestsWithTime(context)
                .onEach {
                    contestsState.value = it.contests
                    currentTimeState.value = it.currentTime
                }.launchIn(this)
        }
    }

    return Pair(contestsState, currentTimeState)
}