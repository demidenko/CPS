package com.demich.cps.contests.loading_engine.loaders

import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.loading.ContestDateConstraints
import com.demich.cps.contests.loading.ContestsLoaderType
import com.demich.cps.platforms.api.codeforces.CodeforcesApi

class CodeforcesContestsLoader: ContestsLoader(type = ContestsLoaderType.codeforces_api) {
    override suspend fun loadContests(
        platform: Contest.Platform,
        dateConstraints: ContestDateConstraints
    ) = CodeforcesApi.getContests()
        .filter { dateConstraints.check(startTime = it.startTime, duration = it.duration) }
        .map { contest ->
            Contest(
                platform = Contest.Platform.codeforces,
                id = contest.id.toString(),
                title = contest.name,
                startTime = contest.startTime,
                duration = contest.duration,
                link = CodeforcesApi.urls.contest(contestId = contest.id)
            )
        }
}