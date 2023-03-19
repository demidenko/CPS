package com.demich.cps.contests.loaders

import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.database.ContestsListDao
import com.demich.cps.utils.LoadingStatus

class ContestsReceiver(
    private val dao: ContestsListDao,
    private val setLoadingStatus: (Contest.Platform, LoadingStatus) -> Unit,
    val consumeError: (Contest.Platform, ContestsLoaders, Throwable) -> Unit,
    private val clearErrors: (Contest.Platform) -> Unit,
) {
    fun startLoading(platform: Contest.Platform) {
        setLoadingStatus(platform, LoadingStatus.LOADING)
        clearErrors(platform)
    }

    suspend fun finishSuccess(platform: Contest.Platform, contests: List<Contest>) {
        setLoadingStatus(platform, LoadingStatus.PENDING)
        dao.replace(
            platform = platform,
            contests = contests.map(::titleFix)
        )
    }

    fun finishFailed(platform: Contest.Platform) {
        setLoadingStatus(platform, LoadingStatus.FAILED)
    }
}

private fun titleFix(contest: Contest): Contest {
    val fixedTitle = when (contest.platform) {
        Contest.Platform.atcoder -> {
            contest.title
                .replace("（", " (")
                .replace("）",") ")
        }
        else -> contest.title
    }.trim()
    return if (contest.title == fixedTitle) contest
    else contest.copy(title = fixedTitle)
}