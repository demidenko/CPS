package com.demich.cps.contests.loaders

import android.content.Context
import com.demich.cps.contests.Contest
import com.demich.cps.utils.codeforces.CodeforcesApi

class CodeforcesContestsLoader: ContestsLoader(
    supportedPlatforms = setOf(Contest.Platform.codeforces),
    type = ContestsLoaders.codeforces
) {
    override suspend fun loadContests(platform: Contest.Platform, context: Context): List<Contest> {
        return CodeforcesApi.getContests().map { contest ->
            Contest(
                platform = Contest.Platform.codeforces,
                id = contest.id.toString(),
                title = contest.name,
                startTime = contest.startTime,
                durationSeconds = contest.duration.inWholeSeconds,
                link = CodeforcesApi.urls.contest(contestId = contest.id)
            )
        }
    }

}