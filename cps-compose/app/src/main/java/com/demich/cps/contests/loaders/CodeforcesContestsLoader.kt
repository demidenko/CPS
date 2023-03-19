package com.demich.cps.contests.loaders

import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.settings.ContestDateConstraints
import com.demich.cps.platforms.api.CodeforcesApi

class CodeforcesContestsLoader: ContestsLoader(type = ContestsLoaders.codeforces) {
    override suspend fun loadContests(
        platform: Contest.Platform,
        dateConstraints: ContestDateConstraints.Current
    ) = CodeforcesApi.getContests()
        .filter { dateConstraints.check(startTime = it.startTime, duration = it.duration) }
        .map { contest ->
            Contest(
                platform = Contest.Platform.codeforces,
                id = contest.id.toString(),
                title = contest.name,
                startTime = contest.startTime,
                durationSeconds = contest.duration.inWholeSeconds,
                link = CodeforcesApi.urls.contestPending(contestId = contest.id)
            )
        }
}