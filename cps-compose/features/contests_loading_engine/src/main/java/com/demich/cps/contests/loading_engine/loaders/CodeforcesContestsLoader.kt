package com.demich.cps.contests.loading_engine.loaders

import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.loading.ContestDateConstraints
import com.demich.cps.contests.loading.ContestsLoaderType
import com.demich.cps.platforms.api.codeforces.CodeforcesApi
import com.demich.cps.platforms.api.codeforces.CodeforcesUrls

class CodeforcesContestsLoader(val api: CodeforcesApi): ContestsLoader() {
    override val type get() = ContestsLoaderType.codeforces_api

    override suspend fun loadContests(
        platform: Contest.Platform,
        dateConstraints: ContestDateConstraints
    ) = api.getContests()
        .mapNotNull {
            if (dateConstraints.check(startTime = it.startTime, duration = it.duration)) {
                Contest(
                    platform = Contest.Platform.codeforces,
                    id = it.id.toString(),
                    title = it.name,
                    startTime = it.startTime,
                    duration = it.duration,
                    link = CodeforcesUrls.contest(contestId = it.id)
                )
            } else {
                null
            }
        }
}