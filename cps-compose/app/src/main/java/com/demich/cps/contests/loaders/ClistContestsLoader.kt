package com.demich.cps.contests.loaders

import com.demich.cps.contests.Contest
import com.demich.cps.contests.settings.ContestDateConstraints
import com.demich.cps.utils.CListUtils
import com.demich.cps.platforms.api.ClistApi
import com.demich.cps.platforms.api.ClistContest
import kotlinx.datetime.Instant

class ClistContestsLoader(
    val apiAccess: ClistApi.ApiAccess,
    val includeResourceIds: suspend () -> Collection<Int>
): ContestsLoaderMultiple(type = ContestsLoaders.clist) {
    override suspend fun loadContests(
        platforms: Set<Contest.Platform>,
        dateConstraints: ContestDateConstraints.Current
    ) = ClistApi.getContests(
        apiAccess = apiAccess,
        maxStartTime = dateConstraints.maxStartTime,
        minEndTime = dateConstraints.minEndTime,
        resourceIds = CListUtils.makeResourceIds(platforms, includeResourceIds)
    ).mapAndFilterResult(dateConstraints)
}


private fun Collection<ClistContest>.mapAndFilterResult(dateConstraints: ContestDateConstraints.Current) =
    mapNotNull { clistContest ->
        val contest = clistContest.toContest()
        if (!dateConstraints.check(startTime = contest.startTime, duration = contest.duration)) return@mapNotNull null
        when (contest.platform) {
            Contest.Platform.atcoder -> contest.takeIf { clistContest.host == "atcoder.jp" }
            else -> contest
        }
    }

private fun ClistContest.toContest(): Contest {
    val platform = Contest.platformsExceptUnknown
        .find { CListUtils.getClistApiResourceId(it) == resource_id }
        ?: Contest.Platform.unknown

    return Contest(
        platform = platform,
        id = CListUtils.extractContestId(this, platform),
        title = event,
        startTime = Instant.parse(start+"Z"),
        endTime = Instant.parse(end+"Z"),
        link = href
    )
}