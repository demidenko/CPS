package com.demich.cps.contests.loading_engine.fetchers

import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.database.ContestPlatform
import com.demich.cps.contests.database.toContestPlatform
import com.demich.cps.contests.database.toGeneralPlatformOrNull
import com.demich.cps.contests.fetching.ContestDateConstraints
import com.demich.cps.contests.fetching.ContestsFetchSource
import com.demich.cps.platforms.Platform
import com.demich.cps.platforms.api.clist.ClistApi
import com.demich.cps.platforms.api.clist.ClistContest
import com.demich.cps.platforms.api.clist.ClistResource
import com.demich.cps.platforms.utils.ClistContestDateParser
import com.demich.cps.platforms.utils.clistResourceId
import com.demich.cps.platforms.utils.extractContestId
import kotlin.time.Duration.Companion.seconds

class ClistContestsFetcher(
    val api: ClistApi,
    val resources: Collection<ClistResource>
): ContestsMultiplatformFetcher() {
    override val fetchSource get() = ContestsFetchSource.clist_api

    override suspend fun getContests(
        platforms: Set<ContestPlatform>,
        dateConstraints: ContestDateConstraints
    ): List<Contest> {
        val resourceIds = makeResourceIds(
            platforms = platforms.mapNotNull { it.toGeneralPlatformOrNull() },
            additionalResources = if (platforms.contains(unknown)) resources else emptyList()
        )

        return api.getContests(
            maxStartTime = dateConstraints.maxStartTime,
            minEndTime = dateConstraints.minEndTime,
            resourceIds = resourceIds
        ).mapAndFilterResult(dateConstraints)
    }
}

private fun makeResourceIds(
    platforms: Collection<Platform>,
    additionalResources: Collection<ClistResource>
): Set<Int> = buildSet {
    platforms.forEach { add(requireNotNull(it.clistResourceId)) }
    additionalResources.forEach { add(it.id) }
}

private fun Collection<ClistContest>.mapAndFilterResult(
    dateConstraints: ContestDateConstraints
): List<Contest> =
    context(ClistContestDateParser()) {
        mapNotNull { clistContest ->
            val contest = clistContest.toContest()
            if (!dateConstraints.check(contest)) {
                return@mapNotNull null
            }
            when (contest.platform) {
                atcoder -> contest.takeIf { clistContest.host == "atcoder.jp" }
                else -> contest
            }
        }
    }

context(parser: ClistContestDateParser)
private fun ClistContest.toContest(): Contest {
    val platform: ContestPlatform =
        Platform.entries.find { it.clistResourceId == resource_id }
            ?.toContestPlatform() ?: unknown

    return Contest(
        platform = platform,
        id = extractContestId(platform),
        title = event,
        startTime = parser.parse(start),
        endTime = parser.parse(end),
        duration = duration.seconds,
        link = href,
        host = host.takeIf { platform == unknown }
    )
}