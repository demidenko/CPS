package com.demich.cps.contests.loading_engine.fetchers

import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.loading.ContestsFetchSource
import com.demich.cps.platforms.api.dmoj.DmojApi
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class DmojContestsFetcher(val api: DmojApi): ContestsSinglePlatformFetcher() {

    override val fetchSource: ContestsFetchSource get() = dmoj_api

    override suspend fun getContests() =
        api.getContests().map { contest ->
            val startTime = Instant.parse(contest.start_time)
            val endTime = Instant.parse(contest.end_time)
            Contest(
                platform = dmoj,
                id = contest.key,
                title = contest.name,
                startTime = startTime,
                endTime = endTime,
                duration = contest.time_limit?.seconds ?: (endTime - startTime)
            )
        }
}