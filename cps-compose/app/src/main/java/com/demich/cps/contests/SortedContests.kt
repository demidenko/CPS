package com.demich.cps.contests

import android.content.Context
import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.database.contestsListDao
import com.demich.cps.utils.flowOfFlooredCurrentTime
import com.demich.cps.utils.isSortedWith
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.seconds

internal data class SortedContests(
    val contests: List<Contest>,
    val currentTime: Instant
)

private fun flowOfContests(context: Context) =
    context.contestsListDao.flowOfContests()
        .distinctUntilChanged()
        .combine(ContestsInfoDataStore(context).ignoredContests.flow) { list, ignored ->
            if (ignored.isEmpty()) list
            else list.filter { contest -> contest.compositeId !in ignored }
        }

internal fun flowOfSortedContestsWithTime(context: Context): Flow<SortedContests> {
    var last: List<Contest> = emptyList()
    var sortedLast: List<Contest> = emptyList()
    return combineTransform(
        flow = flowOfContests(context),
        flow2 = flowOfFlooredCurrentTime(1.seconds)
    ) { contests, currentTime ->
        if (last != contests) {
            last = contests
            sortedLast = contests
        }
        val comparator = Contest.getComparator(currentTime)
        if (!sortedLast.isSortedWith(comparator)) {
            sortedLast = sortedLast.sortedWith(comparator)
        }
        emit(SortedContests(sortedLast, currentTime))
    }
}