package com.demich.cps.contests.loaders

import android.content.Context
import com.demich.cps.contests.Contest
import com.demich.cps.contests.settings.settingsContests
import com.demich.cps.utils.CListApi
import com.demich.cps.utils.ClistContest
import com.demich.cps.utils.getCurrentTime
import kotlin.time.Duration.Companion.days

class ClistContestsLoader: ContestsLoaderMultiple(
    supportedPlatforms = Contest.platforms.toSet()
) {
    override suspend fun loadContests(platforms: Set<Contest.Platform>, context: Context): List<Contest> {
        val settings = context.settingsContests
        val now = getCurrentTime()
        val contests = CListApi.getContests(
            apiAccess = settings.clistApiAccess(),
            platforms = platforms,
            maxStartTime = now + 120.days,
            minEndTime = now - 7.days,
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