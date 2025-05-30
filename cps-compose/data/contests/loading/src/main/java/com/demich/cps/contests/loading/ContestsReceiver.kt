package com.demich.cps.contests.loading

import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.database.ContestsListDao

fun interface ContestsReceiver {
    suspend fun save(platform: Contest.Platform, contests: List<Contest>)
}

data class ContestsLoadingResult(
    val platform: Contest.Platform,
    val loaderType: ContestsLoaderType,
    val result: Result<List<Contest>>
)

fun ContestsListDao.asContestsReceiver() =
    ContestsReceiver { platform, contests ->
        replace(platform = platform, contests = contests)
    }
