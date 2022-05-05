package com.demich.cps.contests.loaders

import com.demich.cps.contests.Contest
import com.demich.cps.contests.settings.ContestTimePrefs
import com.demich.cps.utils.DmojApi
import kotlinx.datetime.Instant

class DmojContestsLoader: ContestsLoader(ContestsLoaders.dmoj) {
    override suspend fun loadContests(
        platform: Contest.Platform,
        timeLimits: ContestTimePrefs.Limits
    ): List<Contest> {
        return DmojApi.getContests().map {
            Contest(
                platform = Contest.Platform.dmoj,
                id = it.key,
                title = it.name,
                startTime = Instant.parse(it.start_time),
                endTime = Instant.parse(it.end_time)
            )
        }
    }

}