package com.demich.cps.community.codeforces

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demich.cps.accounts.userinfo.CodeforcesUserInfo
import com.demich.cps.community.settings.settingsCommunity
import com.demich.cps.platforms.api.CodeforcesApi
import com.demich.cps.platforms.api.CodeforcesBlogEntry
import com.demich.cps.platforms.api.CodeforcesLocale
import com.demich.cps.platforms.utils.codeforces.CodeforcesRecent
import com.demich.cps.platforms.utils.codeforces.CodeforcesUtils
import com.demich.cps.community.follow.followListDao
import com.demich.cps.platforms.api.CodeforcesColorTag
import com.demich.cps.utils.LoadingStatus
import com.demich.cps.utils.awaitPair
import com.demich.cps.utils.backgroundDataLoader
import com.demich.cps.utils.combine
import com.demich.cps.utils.sharedViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
            CodeforcesTitle.MAIN -> mainBlogEntries.loadingStatusFlow
            CodeforcesTitle.TOP -> {
                listOf(topBlogEntries.loadingStatusFlow, topComments.loadingStatusFlow)
                    .combine()
            }
            CodeforcesTitle.RECENT -> recentActions.loadingStatusFlow
            else -> flowOf(LoadingStatus.PENDING)
        }
    }

    private val mainBlogEntries = dataLoader(emptyList()) { getBlogEntries(page = "/", locale = it) }
    override fun flowOfMainBlogEntries(context: Context) = mainBlogEntries.getDataFlow(context)

    private val topBlogEntries = dataLoader(emptyList()) { getBlogEntries(page = "/top", locale = it) }
    override fun flowOfTopBlogEntries(context: Context) = topBlogEntries.getDataFlow(context)

    private val topComments = dataLoader(emptyList()) { getComments(page = "/topComments?days=2", locale = it) }
    override fun flowOfTopComments(context: Context) = topComments.getDataFlow(context)

    private val recentActions = dataLoader(CodeforcesRecent(emptyList(), emptyList())) { getRecentActions(locale = it) }
    override fun flowOfRecent(context: Context) = recentActions.getDataFlow(context)

    private fun reload(title: CodeforcesTitle, locale: CodeforcesLocale) {
        when (title) {
            CodeforcesTitle.MAIN -> mainBlogEntries.launchLoadIfActive(locale)
            CodeforcesTitle.TOP -> {
                topBlogEntries.launchLoadIfActive(locale)
                //TODO: set comments inactive after many reloads without showing them
                topComments.launchLoadIfActive(locale)
            }
            CodeforcesTitle.RECENT -> recentActions.launchLoadIfActive(locale)
            else -> return
        }
    }

    override fun reload(title: CodeforcesTitle, context: Context) {
        viewModelScope.launch {
            val locale = context.settingsCommunity.codeforcesLocale()
            reload(title = title, locale = locale)
        }
    }

    private val reloadableTitles get() = listOf(
        CodeforcesTitle.MAIN,
        CodeforcesTitle.TOP,
        CodeforcesTitle.RECENT
    )

    override fun reloadAll(context: Context) {
        viewModelScope.launch {
            val locale = context.settingsCommunity.codeforcesLocale()
            reloadableTitles.forEach { reload(title = it, locale = locale) }
        }
    }

    private suspend fun getBlogEntries(page: String, locale: CodeforcesLocale) =
        CodeforcesUtils.extractBlogEntries(source = CodeforcesApi.getPageSource(path = page, locale = locale))

    private suspend fun getComments(page: String, locale: CodeforcesLocale) =
        CodeforcesUtils.extractComments(source = CodeforcesApi.getPageSource(path = page, locale = locale))

    private suspend fun getRecentActions(locale: CodeforcesLocale) =
        CodeforcesUtils.extractRecentActions(source = CodeforcesApi.getPageSource(path = "/recent-actions", locale = locale))

    fun addToFollowList(userInfo: CodeforcesUserInfo, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            context.followListDao.addNewUser(userInfo)
        }
    }

    override fun addToFollowList(handle: String, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            context.settingsCommunity.codeforcesFollowEnabled(newValue = true)
            context.followListDao.addNewUser(handle)
        }
    }

    private val followLoadingStatus = MutableStateFlow(LoadingStatus.PENDING)
    fun flowOfFollowUpdateLoadingStatus(): StateFlow<LoadingStatus> = followLoadingStatus
    override fun updateFollowUsersInfo(context: Context) {
        viewModelScope.launch {
            if (!followLoadingStatus.compareAndSet(LoadingStatus.PENDING, LoadingStatus.LOADING)) return@launch
            withContext(Dispatchers.IO) { context.followListDao.updateUsers() }
            followLoadingStatus.value = LoadingStatus.PENDING
        }
    }

    private val blogEntriesLoader = backgroundDataLoader<List<CodeforcesBlogEntry>>()
    fun flowOfBlogEntriesResult(handle: String, context: Context, key: Int) =
        blogEntriesLoader.execute(id = "$handle#$key") {
            val (result, colorTag) = awaitPair(
                blockFirst = { context.followListDao.getAndReloadBlogEntries(handle) },
                blockSecond = { CodeforcesUtils.getRealColorTagOrNull(handle) }
            )
            result.getOrThrow().map {
                it.copy(
                    title = CodeforcesUtils.extractTitle(it),
                    authorColorTag = colorTag ?: CodeforcesColorTag.BLACK
                )
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
    fun getDataFlow(context: Context): StateFlow<T> {
        if (inactive) {
            inactive = false
            scope.launch {
                launchLoadIfActive(locale = context.settingsCommunity.codeforcesLocale())
            }
        }
        return dataFlow
    }

    private val loadingStatus = MutableStateFlow(LoadingStatus.PENDING)
    val loadingStatusFlow: StateFlow<LoadingStatus> get() = loadingStatus

    fun launchLoadIfActive(locale: CodeforcesLocale) {
        if (inactive) return
        loadingStatus.update {
            require(it != LoadingStatus.LOADING)
            LoadingStatus.LOADING
        }
        scope.launch {
            withContext(Dispatchers.IO) {
                kotlin.runCatching { getData(locale) }
            }.onFailure {
                loadingStatus.value = LoadingStatus.FAILED
            }.onSuccess {
                dataFlow.value = it
                loadingStatus.value = LoadingStatus.PENDING
            }
        }
    }
}

private fun <T: Any> ViewModel.dataLoader(init: T, getData: suspend (CodeforcesLocale) -> T) =
    CodeforcesDataLoader(scope = viewModelScope, init = init, getData = getData)