package com.demich.cps.contests.loading_engine.loaders

import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.database.ContestPlatform
import com.demich.cps.contests.loading.ContestDateConstraints
import com.demich.cps.contests.loading.ContestsFetchSource
import com.demich.cps.platforms.api.codeforces.CodeforcesApi
import com.demich.cps.platforms.api.codeforces.CodeforcesUrls

class CodeforcesContestsFetcher(val api: CodeforcesApi): ContestsFetcher() {
    override val fetchSource get() = ContestsFetchSource.codeforces_api

    override suspend fun getContests(
        platform: ContestPlatform,
        dateConstraints: ContestDateConstraints
    ) = api.getContests()
        .mapNotNull {
            if (dateConstraints.check(startTime = it.startTime, duration = it.duration)) {
                Contest(
                    platform = codeforces,
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