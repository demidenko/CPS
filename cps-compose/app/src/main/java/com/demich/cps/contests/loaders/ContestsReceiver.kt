package com.demich.cps.contests.loaders

import com.demich.cps.contests.Contest
import com.demich.cps.room.ContestsListDao
import com.demich.cps.utils.LoadingStatus
import kotlinx.coroutines.flow.MutableStateFlow

class ContestsReceiver(
    private val dao: ContestsListDao,
    private val getLoadingStatusState: (Contest.Platform) -> MutableStateFlow<LoadingStatus>,
    private val getErrorsListState: (Contest.Platform) -> MutableStateFlow<List<Pair<ContestsLoaders, Throwable>>>,
) {
    fun startLoading(platform: Contest.Platform) {
        val loadingStatusState = getLoadingStatusState(platform)
        require(loadingStatusState.value != LoadingStatus.LOADING)
        loadingStatusState.value = LoadingStatus.LOADING
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