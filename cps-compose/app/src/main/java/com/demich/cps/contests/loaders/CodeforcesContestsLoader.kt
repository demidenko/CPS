package com.demich.cps.contests.loaders

import com.demich.cps.contests.Contest
import com.demich.cps.utils.codeforces.CodeforcesApi

class CodeforcesContestsLoader: ContestsLoader(type = ContestsLoaders.codeforces) {
    override suspend fun loadContests(platform: Contest.Platform) =
        CodeforcesApi.getContests().map { contest ->
            Contest(
                platform = Contest.Platform.codeforces,
                id = contest.id.toString(),
                title = contest.name,
                startTime = contest.startTime,
                durationSeconds = contest.duration.inWholeSeconds,
                link = CodeforcesApi.urls.contestWaiting(contestId = contest.id)
            )
        }
}