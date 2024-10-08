package com.demich.cps.contests.loading_engine.loaders

import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.loading.ContestsLoaderType
import com.demich.cps.platforms.api.DmojApi
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.seconds

class DmojContestsLoader: ContestsLoader(ContestsLoaderType.dmoj_api) {
    override suspend fun loadContests(platform: Contest.Platform) =
        DmojApi.getContests().map { contest ->
            val startTime = Instant.parse(contest.start_time)
            val endTime = Instant.parse(contest.end_time)
            Contest(
                platform = Contest.Platform.dmoj,
                id = contest.key,
                title = contest.name,
                startTime = startTime,
                endTime = endTime,
                duration = contest.time_limit?.seconds ?: (endTime - startTime)
            )
        }
}