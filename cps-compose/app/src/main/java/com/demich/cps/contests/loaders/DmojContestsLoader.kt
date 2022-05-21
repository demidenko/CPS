package com.demich.cps.contests.loaders

import com.demich.cps.contests.Contest
import com.demich.cps.utils.DmojApi
import kotlinx.datetime.Instant

class DmojContestsLoader: ContestsLoader(ContestsLoaders.dmoj) {
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