package com.demich.cps.contests.loading

import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.database.ContestsListDao

interface ContestsReceiver {
    fun onStartLoading(platform: Contest.Platform)

    fun onFinish(platform: Contest.Platform)

    suspend fun onResult(loadingResult: ContestsLoadingResult)
}

data class ContestsLoadingResult(
    val platform: Contest.Platform,
    val loaderType: ContestsLoaderType,
    val result: Result<List<Contest>>
)

fun ContestsListDao.asContestsReceiver(
    onStartLoading: (Contest.Platform) -> Unit = {},
    onFinish: (Contest.Platform) -> Unit = {},
    onResult: (ContestsLoadingResult) -> Unit = {}
) = object : ContestsReceiver {
    override fun onStartLoading(platform: Contest.Platform) = onStartLoading(platform)
    override fun onFinish(platform: Contest.Platform) = onFinish(platform)
    override suspend fun onResult(loadingResult: ContestsLoadingResult) {
        loadingResult.apply {
            result.onSuccess { replace(platform = platform, contests = it) }
        }
        onResult(loadingResult)
    }
}