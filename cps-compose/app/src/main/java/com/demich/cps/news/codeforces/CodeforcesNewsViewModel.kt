package com.demich.cps.news.codeforces

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demich.cps.accounts.userinfo.CodeforcesUserInfo
import com.demich.cps.news.settings.settingsNews
import com.demich.cps.platforms.api.CodeforcesApi
import com.demich.cps.platforms.api.CodeforcesBlogEntry
import com.demich.cps.platforms.api.CodeforcesLocale
import com.demich.cps.platforms.utils.codeforces.CodeforcesUtils
import com.demich.cps.room.followListDao
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
fun codeforcesNewsViewModel(): CodeforcesNewsViewModel = sharedViewModel()

class CodeforcesNewsViewModel: ViewModel(), CodeforcesNewsDataManger {

    private val reloadableTitles = listOf(
        CodeforcesTitle.MAIN,
        CodeforcesTitle.TOP,
        CodeforcesTitle.RECENT
    )

    override fun flowOfLoadingStatus(): Flow<LoadingStatus> =
        listOf(
            mainBlogEntries.loadingStatusState,
            topBlogEntries.loadingStatusState,
            topComments.loadingStatusState,
            recentActions.loadingStatusState
        ).combine()

    override fun flowOfLoadingStatus(title: CodeforcesTitle): Flow<LoadingStatus> {
        return when (title) {
            CodeforcesTitle.MAIN -> mainBlogEntries.loadingStatusState
            CodeforcesTitle.TOP -> {
                listOf(topBlogEntries.loadingStatusState, topComments.loadingStatusState)
                    .combine()
            }
            CodeforcesTitle.RECENT -> recentActions.loadingStatusState
            else -> flowOf(LoadingStatus.PENDING)
        }
    }

    private val mainBlogEntries = dataLoader(emptyList()) { loadBlogEntries(page = "/", locale = it) }
    override fun flowOfMainBlogEntries(context: Context) = mainBlogEntries.getDataFlow(context)

    private val topBlogEntries = dataLoader(emptyList()) { loadBlogEntries(page = "/top", locale = it) }
    override fun flowOfTopBlogEntries(context: Context) = topBlogEntries.getDataFlow(context)

    private val topComments = dataLoader(emptyList()) { loadComments(page = "/topComments?days=2", locale = it) }
    override fun flowOfTopComments(context: Context) = topComments.getDataFlow(context)

    private val recentActions = dataLoader(Pair(emptyList(), emptyList())) { loadRecentActions(locale = it) }
    override fun flowOfRecentActions(context: Context) = recentActions.getDataFlow(context)

    private fun reload(title: CodeforcesTitle, locale: CodeforcesLocale) {
        when(title) {
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
            val locale = context.settingsNews.codeforcesLocale()
            reload(title = title, locale = locale)
        }
    }

    override fun reloadAll(context: Context) {
        viewModelScope.launch {
            val locale = context.settingsNews.codeforcesLocale()
            reloadableTitles.forEach { reload(title = it, locale = locale) }
        }
    }

    private suspend fun loadBlogEntries(page: String, locale: CodeforcesLocale) =
        CodeforcesUtils.extractBlogEntries(source = CodeforcesApi.getPageSource(path = page, locale = locale))

    private suspend fun loadComments(page: String, locale: CodeforcesLocale) =
        CodeforcesUtils.extractComments(source = CodeforcesApi.getPageSource(path = page, locale = locale))

    private suspend fun loadRecentActions(locale: CodeforcesLocale) =
        CodeforcesUtils.extractRecentActions(source = CodeforcesApi.getPageSource(path = "/recent-actions", locale = locale))

    fun addToFollowList(userInfo: CodeforcesUserInfo, context: Context) {
        viewModelScope.launch {
            context.followListDao.addNewUser(userInfo)
        }
    }

    override fun addToFollowList(handle: String, context: Context) {
        viewModelScope.launch {
            context.settingsNews.codeforcesFollowEnabled(newValue = true)
            context.followListDao.addNewUser(handle)
        }
    }

    private val followLoadingStatus = MutableStateFlow(LoadingStatus.PENDING)
    fun flowOfFollowUpdateLoadingStatus(): StateFlow<LoadingStatus> = followLoadingStatus
    override fun updateFollowUsersInfo(context: Context) {
        viewModelScope.launch {
            if (!followLoadingStatus.compareAndSet(LoadingStatus.PENDING, LoadingStatus.LOADING)) return@launch
            context.followListDao.updateUsers()
            followLoadingStatus.value = LoadingStatus.PENDING
        }
    }

    private val blogEntriesLoader = backgroundDataLoader<List<CodeforcesBlogEntry>>()
    fun flowOfBlogEntriesResult(handle: String, context: Context, id: Long) =
        blogEntriesLoader.run {
            execute(id = "$handle#$id") {
                val (result, colorTag) = awaitPair(
                    context = Dispatchers.IO,
                    blockFirst = { context.followListDao.getAndReloadBlogEntries(handle) },
                    blockSecond = { CodeforcesUtils.getRealColorTag(handle) }
                )
                result.getOrThrow().map {
                    it.copy(
                        title = CodeforcesUtils.extractTitle(it),
                        authorColorTag = colorTag
                    )
                }
            }
            flowOfResult()
        }
}

private class CodeforcesDataLoader<T>(
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
                launchLoadIfActive(locale = context.settingsNews.codeforcesLocale())
            }
        }
        return dataFlow
    }

    val loadingStatusState = MutableStateFlow(LoadingStatus.PENDING)

    fun launchLoadIfActive(locale: CodeforcesLocale) {
        if (inactive) return
        loadingStatusState.update {
            require(it != LoadingStatus.LOADING)
            LoadingStatus.LOADING
        }
        scope.launch {
            withContext(Dispatchers.IO) {
                kotlin.runCatching { getData(locale) }
            }.onFailure {
                loadingStatusState.value = LoadingStatus.FAILED
            }.onSuccess {
                dataFlow.value = it
                loadingStatusState.value = LoadingStatus.PENDING
            }
        }
    }
}

private fun<T> ViewModel.dataLoader(init: T, getData: suspend (CodeforcesLocale) -> T) =
    CodeforcesDataLoader(scope = viewModelScope, init = init, getData = getData)