package com.demich.cps.community.codeforces

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demich.cps.community.follow.followRepository
import com.demich.cps.community.settings.settingsCommunity
import com.demich.cps.features.codeforces.follow.database.updateFailedBlogEntries
import com.demich.cps.platforms.api.codeforces.models.CodeforcesLocale
import com.demich.cps.platforms.clients.codeforces.CodeforcesClient
import com.demich.cps.platforms.utils.codeforces.CodeforcesRecentFeed
import com.demich.cps.platforms.utils.codeforces.CodeforcesUtils
import com.demich.cps.platforms.utils.codeforces.getRecentFeed
import com.demich.cps.profiles.userinfo.CodeforcesUserInfo
import com.demich.cps.profiles.userinfo.ProfileResult
import com.demich.cps.utils.LoadingStatus
import com.demich.cps.utils.combine
import com.demich.cps.utils.sharedViewModel
import com.demich.datastore_itemized.DataStoreValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

    private val mainBlogEntries = dataLoader(emptyList()) { getMainBlogEntries(locale = it) }
    override fun flowOfMainBlogEntries(context: Context) = mainBlogEntries.flowOfData(context)

    private val topBlogEntries = dataLoader(emptyList()) { getTopBlogEntries(locale = it) }
    override fun flowOfTopBlogEntries(context: Context) = topBlogEntries.flowOfData(context)

    private val topComments = dataLoader(emptyList()) { getTopComments(locale = it) }
    override fun flowOfTopComments(context: Context) = topComments.flowOfData(context)

    private val recentActions = dataLoader(CodeforcesRecentFeed(emptyList(), emptyList())) {
        CodeforcesClient(locale = it).getRecentFeed()
    }
    override fun flowOfRecent(context: Context) = recentActions.flowOfData(context)

    private fun reload(title: CodeforcesTitle, localeItem: DataStoreValue<CodeforcesLocale>) {
        when (title) {
            MAIN -> mainBlogEntries.launchLoadIfActive(localeItem)
            TOP -> {
                topBlogEntries.launchLoadIfActive(localeItem)
                //TODO: set comments inactive after many reloads without showing them
                topComments.launchLoadIfActive(localeItem)
            }
            RECENT -> recentActions.launchLoadIfActive(localeItem)
            else -> return
        }
    }

    override fun reload(titles: List<CodeforcesTitle>, context: Context) {
        val localeItem = context.settingsCommunity.codeforcesLocale
        titles.forEach { reload(title = it, localeItem = localeItem) }
    }

    private suspend fun getMainBlogEntries(locale: CodeforcesLocale) =
        CodeforcesUtils.extractBlogEntries(source = CodeforcesClient(locale = locale).getMainPage())

    private suspend fun getTopBlogEntries(locale: CodeforcesLocale) =
        CodeforcesUtils.extractBlogEntries(source = CodeforcesClient(locale = locale).getTopBlogEntriesPage())

    private suspend fun getTopComments(locale: CodeforcesLocale) =
        CodeforcesUtils.extractComments(source = CodeforcesClient(locale = locale).getTopCommentsPage())

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
                updateUsers()
                updateFailedBlogEntries()
            }
            followLoadingStatus.value = PENDING
        }
    }
}

private class CodeforcesDataLoader<T: Any>(
    val scope: CoroutineScope,
    init: T,
    val getData: suspend (CodeforcesLocale) -> T
) {
    private val dataFlow: MutableStateFlow<T> = MutableStateFlow(init)

    private var inactive = true
    fun flowOfData(context: Context): StateFlow<T> {
        if (inactive) {
            inactive = false
            launchLoadIfActive(localeItem = context.settingsCommunity.codeforcesLocale)
        }
        return dataFlow
    }

    private val loadingStatus = MutableStateFlow(LoadingStatus.PENDING)
    val loadingStatusFlow: StateFlow<LoadingStatus> get() = loadingStatus

    fun launchLoadIfActive(localeItem: DataStoreValue<CodeforcesLocale>) {
        if (inactive) return
        loadingStatus.update {
            if (it == LOADING) return
            LOADING
        }
        scope.launch(Dispatchers.Default) {
            runCatching {
                val locale = localeItem()
                getData(locale)
            }.onFailure {
                loadingStatus.value = FAILED
            }.onSuccess {
                dataFlow.value = it
                loadingStatus.value = PENDING
            }
        }
    }
}

private fun <T: Any> ViewModel.dataLoader(init: T, getData: suspend (CodeforcesLocale) -> T) =
    CodeforcesDataLoader(scope = viewModelScope, init = init, getData = getData)