package com.demich.cps.contests.loading.loaders

import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.loading.ContestsLoaders
import com.demich.cps.platforms.api.DmojApi
import kotlinx.datetime.Instant

class DmojContestsLoader: ContestsLoader(ContestsLoaders.dmoj_api) {
    override suspend fun loadContests(platform: Contest.Platform) =
        DmojApi.getContests().map { contest ->
            Contest(
                platform = Contest.Platform.dmoj,
                id = contest.key,
                title = contest.name,
                startTime = Instant.parse(contest.start_time),
                endTime = Instant.parse(contest.end_time)
            )
        }
}