package com.demich.cps.community.codeforces

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demich.cps.features.codeforces.follow.database.CodeforcesFollowRepository
import com.demich.cps.features.codeforces.follow.database.updateFailedBlogEntries
import com.demich.cps.fetchstate.FetchState
import com.demich.cps.fetchstate.toFetchResultFlow
import com.demich.cps.platforms.api.codeforces.CodeforcesPageContentProvider
import com.demich.cps.platforms.utils.codeforces.CodeforcesBlogEntriesPageParser
import com.demich.cps.platforms.utils.codeforces.CodeforcesCommentsPageParser
import com.demich.cps.platforms.utils.codeforces.getRecentFeed
import com.demich.cps.utils.FetchValue
import com.demich.cps.utils.LoadingStatus
import com.demich.cps.utils.combine
import com.demich.cps.utils.loadingStatus
import com.demich.cps.utils.plus
import com.demich.cps.utils.sharedViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Composable
fun codeforcesCommunityViewModel(): CodeforcesCommunityViewModel = sharedViewModel()

class CodeforcesCommunityViewModel: ViewModel() {

    private fun Collection<CodeforcesDataLoader<*>>.flowOfLoadingStatus() =
        map { it.flowOfLoadingStatus() }.combine()

    fun flowOfLoadingStatus(): Flow<LoadingStatus> =
        [mainBlogEntries, topBlogEntries, topComments, recentActions].flowOfLoadingStatus()

    fun flowOfLoadingStatus(tab: CodeforcesTab): Flow<LoadingStatus> {
        return when (tab) {
            MAIN -> mainBlogEntries.flowOfLoadingStatus()
            TOP -> [topBlogEntries, topComments].flowOfLoadingStatus()
            RECENT -> recentActions.flowOfLoadingStatus()
            LOST -> throw IllegalArgumentException(tab.name)
        }
    }

    private val mainBlogEntries = dataLoader { it.getMainBlogEntries() }
    fun flowOfMainBlogEntries(provider: suspend () -> CodeforcesPageContentProvider) = mainBlogEntries.flowOfData(provider)

    private val topBlogEntries = dataLoader { it.getTopBlogEntries() }
    fun flowOfTopBlogEntries(provider: suspend () -> CodeforcesPageContentProvider) = topBlogEntries.flowOfData(provider)

    private val topComments = dataLoader { it.getTopComments() }
    fun flowOfTopComments(provider: suspend () -> CodeforcesPageContentProvider) = topComments.flowOfData(provider)

    private val recentActions = dataLoader { it.getRecentFeed() }
    fun flowOfRecent(provider: suspend () -> CodeforcesPageContentProvider) = recentActions.flowOfData(provider)

    private fun reload(tab: CodeforcesTab, provider: suspend () -> CodeforcesPageContentProvider) {
        when (tab) {
            MAIN -> mainBlogEntries.launchLoadIfActive(provider)
            TOP -> {
                topBlogEntries.launchLoadIfActive(provider)
                //TODO: set comments inactive after many reloads without showing them
                topComments.launchLoadIfActive(provider)
            }
            RECENT -> recentActions.launchLoadIfActive(provider)
            else -> return
        }
    }

    fun reload(tabs: List<CodeforcesTab>, provider: suspend () -> CodeforcesPageContentProvider) {
        val provider = viewModelScope.async(
            context = Dispatchers.Default,
            start = LAZY,
            block = { provider() }
        )
        tabs.forEach { reload(tab = it, provider = provider::await) }
    }

    val flowOfFollowUpdateLoadingStatus: StateFlow<LoadingStatus>
        field = MutableStateFlow(PENDING)

    fun updateFollowUsersInfo(repository: CodeforcesFollowRepository) {
        viewModelScope.launch(Dispatchers.Default) {
            if (!flowOfFollowUpdateLoadingStatus.compareAndSet(PENDING, LOADING)) return@launch
            repository.updateProfiles()
            repository.updateFailedBlogEntries()
            flowOfFollowUpdateLoadingStatus.value = PENDING
        }
    }
}

private class CodeforcesDataLoader<T>(
    val scope: CoroutineScope,
    init: T,
    val getData: suspend (CodeforcesPageContentProvider) -> T
) {
    private val flow: MutableStateFlow<FetchValue<T>> = MutableStateFlow(FetchValue(init))
    private val dataFlow: StateFlow<T> = flow.map { it.value }.stateIn(
        initialValue = init,
        scope = scope,
        started = SharingStarted.Eagerly
    )

    private var inactive = true
    fun flowOfData(provider: suspend () -> CodeforcesPageContentProvider): StateFlow<T> {
        if (inactive) {
            inactive = false
            launchLoadIfActive(provider = provider)
        }
        return dataFlow
    }

    fun flowOfLoadingStatus(): Flow<LoadingStatus> =
        flow.map { it.loadingStatus }

    fun launchLoadIfActive(provider: suspend () -> CodeforcesPageContentProvider) {
        if (inactive) return
        flow.update {
            if (it.loadingStatus == LOADING) return
            it + FetchState.Loading
        }
        scope.launch(Dispatchers.Default) {
            flow { emit(getData(provider())) }
                .toFetchResultFlow()
                .collect { result ->
                    flow.update { it + result }
                }
        }
    }
}

private fun <T> ViewModel.dataLoader(init: T, getData: suspend (CodeforcesPageContentProvider) -> T) =
    CodeforcesDataLoader(scope = viewModelScope, init = init, getData = getData)

private fun <T> ViewModel.dataLoader(getData: suspend (CodeforcesPageContentProvider) -> T) =
    CodeforcesDataLoader(scope = viewModelScope, init = null, getData = getData)


private suspend fun CodeforcesPageContentProvider.getMainBlogEntries() =
    CodeforcesBlogEntriesPageParser().parseBlogEntries(page = getMainPage())

private suspend fun CodeforcesPageContentProvider.getTopBlogEntries() =
    CodeforcesBlogEntriesPageParser().parseBlogEntries(page = getTopBlogEntriesPage())

private suspend fun CodeforcesPageContentProvider.getTopComments() =
    CodeforcesCommentsPageParser().parseComments(page = getTopCommentsPage())