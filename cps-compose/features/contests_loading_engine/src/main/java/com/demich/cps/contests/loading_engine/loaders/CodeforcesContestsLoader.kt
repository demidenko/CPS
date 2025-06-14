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
        .mapNotNull {
            if (dateConstraints.check(startTime = it.startTime, duration = it.duration)) {
                Contest(
                    platform = Contest.Platform.codeforces,
                    id = it.id.toString(),
                    title = it.name,
                    startTime = it.startTime,
                    duration = it.duration,
                    link = CodeforcesApi.urls.contest(contestId = it.id)
                )
            } else {
                null
            }
        }
}