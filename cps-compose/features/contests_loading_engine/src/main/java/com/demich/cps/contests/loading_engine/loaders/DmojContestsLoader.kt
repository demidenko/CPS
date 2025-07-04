package com.demich.cps.contests.loading_engine.loaders

import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.loading.ContestsLoaderType
import com.demich.cps.platforms.api.dmoj.DmojApi
import kotlinx.datetime.toDeprecatedInstant
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class DmojContestsLoader(val api: DmojApi): ContestsLoader() {
    override val type get() = ContestsLoaderType.dmoj_api

    override suspend fun loadContests(platform: Contest.Platform) =
        api.getContests().map { contest ->
            val startTime = Instant.parse(contest.start_time)
            val endTime = Instant.parse(contest.end_time)
            Contest(
                platform = Contest.Platform.dmoj,
                id = contest.key,
                title = contest.name,
                startTime = startTime.toDeprecatedInstant(),
                endTime = endTime.toDeprecatedInstant(),
                duration = contest.time_limit?.seconds ?: (endTime - startTime)
            )
        }
}