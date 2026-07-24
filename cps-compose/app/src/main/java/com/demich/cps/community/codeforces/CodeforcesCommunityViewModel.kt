package com.demich.cps.community.codeforces

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demich.cps.community.follow.followRepository
import com.demich.cps.community.settings.settingsCommunity
import com.demich.cps.features.codeforces.follow.database.addNewUser
import com.demich.cps.features.codeforces.follow.database.updateFailedBlogEntries
import com.demich.cps.platforms.api.codeforces.CodeforcesPageContentProvider
import com.demich.cps.platforms.clients.codeforces.CodeforcesClient
import com.demich.cps.platforms.utils.codeforces.CodeforcesBlogEntriesPageParser
import com.demich.cps.platforms.utils.codeforces.CodeforcesCommentsPageParser
import com.demich.cps.platforms.utils.codeforces.CodeforcesRecentFeed
import com.demich.cps.platforms.utils.codeforces.getRecentFeed
import com.demich.cps.profiles.userinfo.CodeforcesUserInfo
import com.demich.cps.profiles.userinfo.ProfileResult
import com.demich.cps.utils.LoadingStatus
import com.demich.cps.utils.combine
import com.demich.cps.utils.sharedViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Composable
fun codeforcesCommunityViewModel(): CodeforcesCommunityViewModel = sharedViewModel()

class CodeforcesCommunityViewModel: ViewModel(), CodeforcesCommunityDataManger {

    override fun flowOfLoadingStatus(): Flow<LoadingStatus> =
        listOf(
            mainBlogEntries.loadingStatusFlow,
            topBlogEntries.loadingStatusFlow,
            topComments.loadingStatusFlow,
            recentActions.loadingStatusFlow
        ).combine()

    override fun flowOfLoadingStatus(title: CodeforcesTitle): Flow<LoadingStatus> {
        return when (title) {
            MAIN -> mainBlogEntries.loadingStatusFlow
            TOP -> {
                listOf(topBlogEntries.loadingStatusFlow, topComments.loadingStatusFlow)
                    .combine()
            }
            RECENT -> recentActions.loadingStatusFlow
            LOST -> throw IllegalArgumentException(title.name)
        }
    }

    private val mainBlogEntries = dataLoader(emptyList()) { it.getMainBlogEntries() }
    override fun flowOfMainBlogEntries(context: Context) = mainBlogEntries.flowOfData(context)

    private val topBlogEntries = dataLoader(emptyList()) { it.getTopBlogEntries() }
    override fun flowOfTopBlogEntries(context: Context) = topBlogEntries.flowOfData(context)

    private val topComments = dataLoader(emptyList()) { it.getTopComments() }
    override fun flowOfTopComments(context: Context) = topComments.flowOfData(context)

    private val recentActions = dataLoader(CodeforcesRecentFeed(emptyList(), emptyList())) { it.getRecentFeed() }
    override fun flowOfRecent(context: Context) = recentActions.flowOfData(context)

    private fun reload(title: CodeforcesTitle, provider: suspend () -> CodeforcesPageContentProvider) {
        when (title) {
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

    override fun reload(titles: List<CodeforcesTitle>, context: Context) {
        val provider = viewModelScope.async(
            context = Dispatchers.Default,
            start = LAZY,
            block = { defaultProvider(context) }
        )
        titles.forEach { reload(title = it, provider = provider::await) }
    }

    fun addToFollowList(result: ProfileResult<CodeforcesUserInfo>, context: Context) {
        viewModelScope.launch(Dispatchers.Default) {
            context.followRepository.addNewUser(result)
        }
    }

    override fun addToFollowList(handle: String, context: Context) {
        viewModelScope.launch(Dispatchers.Default) {
            context.settingsCommunity.codeforcesFollowEnabled.setValue(true)
            context.followRepository.addNewUser(handle)
        }
    }

    private val followLoadingStatus = MutableStateFlow(LoadingStatus.PENDING)
    fun flowOfFollowUpdateLoadingStatus(): StateFlow<LoadingStatus> = followLoadingStatus
    override fun updateFollowUsersInfo(context: Context) {
        viewModelScope.launch(Dispatchers.Default) {
            if (!followLoadingStatus.compareAndSet(PENDING, LOADING)) return@launch
            context.followRepository.run {
                updateProfiles()
                updateFailedBlogEntries()
            }
            followLoadingStatus.value = PENDING
        }
    }
}

private suspend fun defaultProvider(context: Context): CodeforcesPageContentProvider =
    CodeforcesClient(locale = context.settingsCommunity.codeforcesLocale())

private class CodeforcesDataLoader<T>(
    val scope: CoroutineScope,
    init: T,
    val getData: suspend (CodeforcesPageContentProvider) -> T
) {
    private val dataFlow: MutableStateFlow<T> = MutableStateFlow(init)

    private var inactive = true
    fun flowOfData(context: Context): StateFlow<T> {
        if (inactive) {
            inactive = false
            launchLoadIfActive(provider = { defaultProvider(context) })
        }
        return dataFlow
    }

    private val loadingStatus = MutableStateFlow(LoadingStatus.PENDING)
    val loadingStatusFlow: StateFlow<LoadingStatus> get() = loadingStatus

    fun launchLoadIfActive(provider: suspend () -> CodeforcesPageContentProvider) {
        if (inactive) return
        loadingStatus.update {
            if (it == LOADING) return
            LOADING
        }
        scope.launch(Dispatchers.Default) {
            runCatching {
                getData(provider())
            }.onFailure {
                loadingStatus.value = FAILED
            }.onSuccess {
                dataFlow.value = it
                loadingStatus.value = PENDING
            }
        }
    }
}

private fun <T> ViewModel.dataLoader(init: T, getData: suspend (CodeforcesPageContentProvider) -> T) =
    CodeforcesDataLoader(scope = viewModelScope, init = init, getData = getData)


private suspend fun CodeforcesPageContentProvider.getMainBlogEntries() =
    CodeforcesBlogEntriesPageParser().parseBlogEntries(page = getMainPage())

private suspend fun CodeforcesPageContentProvider.getTopBlogEntries() =
    CodeforcesBlogEntriesPageParser().parseBlogEntries(page = getTopBlogEntriesPage())

private suspend fun CodeforcesPageContentProvider.getTopComments() =
    CodeforcesCommentsPageParser().parseComments(page = getTopCommentsPage())