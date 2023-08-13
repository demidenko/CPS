package com.demich.cps.contests.loading

import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.database.ContestsListDao

interface ContestsReceiver {
    fun onStartLoading(platform: Contest.Platform)

    fun onFinish(platform: Contest.Platform)

    suspend fun onResult(
        platform: Contest.Platform,
        loaderType: ContestsLoaderType,
        result: Result<List<Contest>>
    )
}

fun ContestsListDao.asContestsReceiver(
    onStartLoading: (Contest.Platform) -> Unit = {},
    onFinish: (Contest.Platform) -> Unit = {},
    onResult: (Contest.Platform, ContestsLoaderType, Result<List<Contest>>) -> Unit = { _, _, _ -> }
) = object : ContestsReceiver {
    override fun onStartLoading(platform: Contest.Platform) = onStartLoading(platform)
    override fun onFinish(platform: Contest.Platform) = onFinish(platform)
    override suspend fun onResult(
        platform: Contest.Platform,
        loaderType: ContestsLoaderType,
        result: Result<List<Contest>>
    ) {
        result.onSuccess { replace(platform = platform, contests = it) }
        onResult(platform, loaderType, result)
    }
}