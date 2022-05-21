package com.demich.cps.contests.loaders

import com.demich.cps.contests.Contest
import com.demich.cps.contests.settings.ContestDateConstraints
import com.demich.cps.utils.CListApi
import com.demich.cps.utils.ClistContest

class ClistContestsLoader(
    val apiAccess: CListApi.ApiAccess,
    val includeResourceIds: suspend () -> Collection<Int>
): ContestsLoaderMultiple(type = ContestsLoaders.clist) {
    override suspend fun loadContests(
        platforms: Set<Contest.Platform>,
        dateConstraints: ContestDateConstraints.Current
    ) = CListApi.getContests(
        apiAccess = apiAccess,
        platforms = platforms,
        maxStartTime = dateConstraints.maxStartTime,
        minEndTime = dateConstraints.minEndTime,
        includeResourceIds = includeResourceIds
    ).mapAndFilterResult(dateConstraints)
}


private fun Collection<ClistContest>.mapAndFilterResult(dateConstraints: ContestDateConstraints.Current) =
    mapNotNull { clistContest ->
        val contest = Contest(clistContest)
        if (!dateConstraints.check(startTime = contest.startTime, duration = contest.duration)) return@mapNotNull null
        when (contest.platform) {
            Contest.Platform.atcoder -> contest.takeIf { clistContest.host == "atcoder.jp" }
            else -> contest
        }
    }