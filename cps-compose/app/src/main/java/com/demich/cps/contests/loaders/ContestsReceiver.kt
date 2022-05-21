package com.demich.cps.contests.loaders

import androidx.compose.runtime.*
import com.demich.cps.contests.Contest
import com.demich.cps.room.ContestsListDao
import com.demich.cps.utils.LoadingStatus

class ContestsReceiver(
    private val dao: ContestsListDao,
    private val getLoadingStatusState: (Contest.Platform) -> MutableState<LoadingStatus>,
    private val getErrorsListState: (Contest.Platform) -> MutableState<List<Pair<ContestsLoaders, Throwable>>>,
) {
    fun startLoading(platform: Contest.Platform) {
        var loadingStatus by getLoadingStatusState(platform)
        require(loadingStatus != LoadingStatus.LOADING)
        loadingStatus = LoadingStatus.LOADING
        getErrorsListState(platform).value = emptyList()
    }

    fun consumeError(platform: Contest.Platform, loaderType: ContestsLoaders, e: Throwable) {
        getErrorsListState(platform).value += loaderType to e
    }

    suspend fun finishSuccess(platform: Contest.Platform, contests: List<Contest>) {
        getLoadingStatusState(platform).value = LoadingStatus.PENDING
        dao.replace(
            platform = platform,
            contests = contests.map(::titleFix)
        )
    }

    fun finishFailed(platform: Contest.Platform) {
        getLoadingStatusState(platform).value = LoadingStatus.FAILED
    }
}

private fun titleFix(contest: Contest): Contest {
    val fixedTitle = contest.title
        .replace("（", " (")
        .replace("）",") ")
        .trim()
    return if (contest.title == fixedTitle) contest
    else contest.copy(title = fixedTitle)
}