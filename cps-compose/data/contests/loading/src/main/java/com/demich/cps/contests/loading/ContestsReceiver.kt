package com.demich.cps.contests.loading

import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.database.ContestPlatform
import com.demich.cps.contests.database.ContestsRepository

fun interface ContestsReceiver {
    suspend fun save(platform: ContestPlatform, contests: List<Contest>)
}

data class ContestsFetchResult(
    val platform: ContestPlatform,
    val fetchSource: ContestsFetchSource,
    val result: Result<List<Contest>>
)

fun ContestsRepository.asContestsReceiver() =
    ContestsReceiver { platform, contests ->
        replace(platform = platform, contests = contests)
    }
