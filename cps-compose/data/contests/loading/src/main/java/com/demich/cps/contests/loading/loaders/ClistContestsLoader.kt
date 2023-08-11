package com.demich.cps.contests.loading.loaders

import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.loading.ContestDateConstraints
import com.demich.cps.contests.loading.ContestsLoaders
import com.demich.cps.platforms.utils.ClistUtils
import com.demich.cps.platforms.api.ClistApi
import com.demich.cps.platforms.api.ClistContest
import kotlinx.datetime.Instant

class ClistContestsLoader(
    val apiAccess: ClistApi.ApiAccess,
    val includeResourceIds: suspend () -> Collection<Int>
): ContestsLoaderMultiple(type = ContestsLoaders.clist_api) {
    override suspend fun loadContests(
        platforms: Set<Contest.Platform>,
        dateConstraints: ContestDateConstraints
    ) = ClistApi.getContests(
        apiAccess = apiAccess,
        maxStartTime = dateConstraints.maxStartTime,
        minEndTime = dateConstraints.minEndTime,
        resourceIds = ClistUtils.makeResourceIds(platforms, includeResourceIds)
    ).mapAndFilterResult(dateConstraints)
}


private fun Collection<ClistContest>.mapAndFilterResult(dateConstraints: ContestDateConstraints) =
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
        .find { ClistUtils.getClistApiResourceId(it) == resource_id }
        ?: Contest.Platform.unknown

    return Contest(
        platform = platform,
        id = ClistUtils.extractContestId(this, platform),
        title = event,
        startTime = Instant.parse(start+"Z"),
        endTime = Instant.parse(end+"Z"),
        link = href
    )
}