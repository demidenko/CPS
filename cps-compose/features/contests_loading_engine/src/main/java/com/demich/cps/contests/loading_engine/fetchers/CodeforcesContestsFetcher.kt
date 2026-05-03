package com.demich.cps.contests.loading_engine.fetchers

import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.fetching.ContestDateConstraints
import com.demich.cps.contests.fetching.ContestsFetchSource
import com.demich.cps.platforms.api.codeforces.CodeforcesApi
import com.demich.cps.platforms.api.codeforces.CodeforcesUrls

class CodeforcesContestsFetcher(val api: CodeforcesApi): ContestsSinglePlatformFetcher() {

    override val fetchSource: ContestsFetchSource get() = codeforces_api

    override suspend fun getContests(dateConstraints: ContestDateConstraints) =
        api.getContests(gym = false).mapNotNull {
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