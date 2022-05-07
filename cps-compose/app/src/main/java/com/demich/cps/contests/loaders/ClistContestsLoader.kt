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
    ): List<Contest> {
        val contests = CListApi.getContests(
            apiAccess = apiAccess,
            platforms = platforms,
            maxStartTime = dateConstraints.maxStartTime,
            minEndTime = dateConstraints.minEndTime,
            includeResourceIds = includeResourceIds
        ).mapAndFilterResult()
        return contests
    }
}


private fun Collection<ClistContest>.mapAndFilterResult(): List<Contest> {
    return mapNotNull {
        val contest = Contest(it)
        when (contest.platform) {
            Contest.Platform.atcoder -> {
                if (it.host == "atcoder.jp")
                    contest.copy(title = contest.title.replace("（", " (").replace('）',')'))
                else null
            }
            else -> contest
        }
    }
}