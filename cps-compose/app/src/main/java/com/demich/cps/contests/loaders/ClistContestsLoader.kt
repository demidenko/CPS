package com.demich.cps.contests.loaders

import com.demich.cps.contests.Contest
import com.demich.cps.contests.settings.ContestTimePrefs
import com.demich.cps.contests.settings.ContestsSettingsDataStore
import com.demich.cps.utils.CListApi
import com.demich.cps.utils.ClistContest

class ClistContestsLoader: ContestsLoaderMultiple(
    supportedPlatforms = Contest.platforms.toSet(),
    type = ContestsLoaders.clist
) {
    override suspend fun loadContests(
        platforms: Set<Contest.Platform>,
        timeLimits: ContestTimePrefs.Limits,
        settings: ContestsSettingsDataStore
    ): List<Contest> {
        val contests = CListApi.getContests(
            apiAccess = settings.clistApiAccess(),
            platforms = platforms,
            maxStartTime = timeLimits.maxStartTime,
            minEndTime = timeLimits.minEndTime,
            includeResourceIds = { settings.clistAdditionalResources().map { it.id } }
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