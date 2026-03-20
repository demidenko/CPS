package com.demich.cps.contests.loading

import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.database.ContestsRepository

fun interface ContestsReceiver {
    suspend fun save(platform: Contest.Platform, contests: List<Contest>)
}

data class ContestsFetchResult(
    val platform: Contest.Platform,
    val fetchSource: ContestsFetchSource,
    val result: Result<List<Contest>>
)

fun ContestsRepository.asContestsReceiver() =
    ContestsReceiver { platform, contests ->
        replace(platform = platform, contests = contests)
    }
