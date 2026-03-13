package com.demich.cps.contests.loading_engine.loaders

import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.loading.ContestsFetchSource
import com.demich.cps.platforms.api.dmoj.DmojApi
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class DmojContestsLoader(val api: DmojApi): ContestsLoader() {
    override val fetchSource get() = ContestsFetchSource.dmoj_api

    override suspend fun getContests(platform: Contest.Platform) =
        api.getContests().map { contest ->
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