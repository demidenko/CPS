package com.demich.cps.news.codeforces

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demich.cps.accounts.managers.CodeforcesUserInfo
import com.demich.cps.news.settings.settingsNews
import com.demich.cps.room.followListDao
import com.demich.cps.utils.LoadingStatus
import com.demich.cps.utils.awaitPair
import com.demich.cps.utils.combine
import com.demich.cps.data.api.CodeforcesApi
import com.demich.cps.data.api.CodeforcesBlogEntry
import com.demich.cps.data.api.CodeforcesLocale
import com.demich.cps.data.api.CodeforcesRecentAction
import com.demich.cps.utils.CodeforcesUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

class CodeforcesNewsViewModel: ViewModel() {

    private inner class DataLoader<T>(
        init: T,
        val getData: suspend (CodeforcesLocale) -> T?
    ) {
        private val dataFlow: MutableStateFlow<T> = MutableStateFlow(init)

        private var inactive = true
        fun getDataFlow(context: Context): StateFlow<T> {
            if (inactive) {
                inactive = false
                viewModelScope.launch {
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
            viewModelScope.launch {
                val data = withContext(Dispatchers.IO) {
                    kotlin.runCatching { getData(locale) }.getOrNull()
                }
                if(data == null) loadingStatusState.value = LoadingStatus.FAILED
                else {
                    dataFlow.value = data
                    loadingStatusState.value = LoadingStatus.PENDING
                }
            }
        }
    }


    private val reloadableTitles = listOf(
        CodeforcesTitle.MAIN,
        CodeforcesTitle.TOP,
        CodeforcesTitle.RECENT
    )

    fun flowOfLoadingStatus(): Flow<LoadingStatus> =
        listOf(
            mainBlogEntries.loadingStatusState,
            topBlogEntries.loadingStatusState,
            topComments.loadingStatusState,
            recentActions.loadingStatusState
        ).combine()

    fun flowOfLoadingStatus(title: CodeforcesTitle): Flow<LoadingStatus> {
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

    private val mainBlogEntries = DataLoader(emptyList()) { loadBlogEntries(page = "/", locale = it) }
    fun flowOfMainBlogEntries(context: Context) = mainBlogEntries.getDataFlow(context)

    private val topBlogEntries = DataLoader(emptyList()) { loadBlogEntries(page = "/top", locale = it) }
    fun flowOfTopBlogEntries(context: Context) = topBlogEntries.getDataFlow(context)

    private val topComments = DataLoader(emptyList()) { loadComments(page = "/topComments?days=2", locale = it) }
    fun flowOfTopComments(context: Context) = topComments.getDataFlow(context)

    private val recentActions = DataLoader(Pair(emptyList(), emptyList())) { loadRecentActions(locale = it) }
    fun flowOfRecentActions(context: Context) = recentActions.getDataFlow(context)

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

    fun reload(title: CodeforcesTitle, context: Context) {
        viewModelScope.launch {
            val locale = context.settingsNews.codeforcesLocale()
            reload(title = title, locale = locale)
        }
    }

    fun reloadAll(context: Context) {
        viewModelScope.launch {
            val locale = context.settingsNews.codeforcesLocale()
            reloadableTitles.forEach { reload(title = it, locale = locale) }
        }
    }

    private suspend fun loadBlogEntries(page: String, locale: CodeforcesLocale): List<CodeforcesBlogEntry>? {
        val s = CodeforcesApi.getPageSource(path = page, locale = locale) ?: return null
        return CodeforcesUtils.extractBlogEntries(s)
    }

    private suspend fun loadComments(page: String, locale: CodeforcesLocale): List<CodeforcesRecentAction>? {
        val s = CodeforcesApi.getPageSource(path = page, locale = locale) ?: return null
        return CodeforcesUtils.extractComments(s)
    }

    private suspend fun loadRecentActions(locale: CodeforcesLocale): Pair<List<CodeforcesBlogEntry>,List<CodeforcesRecentAction>>? {
        val s = CodeforcesApi.getPageSource(path = "/recent-actions", locale = locale) ?: return null
        val comments = CodeforcesUtils.extractComments(s)
        //blog entry with low rating disappeared from blogEntries but has comments, need to merge
        val blogEntries = CodeforcesUtils.extractRecentBlogEntries(s).toMutableList()
        val blogEntriesIds = blogEntries.mapTo(mutableSetOf()) { it.id }
        val usedIds = mutableSetOf<Int>()
        var index = 0
        for (comment in comments) {
            val blogEntry = comment.blogEntry!!
            val id = blogEntry.id
            if (id !in blogEntriesIds) {
                blogEntriesIds.add(id)
                if (index < blogEntries.size) {
                    //mark low rated
                    blogEntries.add(
                        index = index,
                        element = blogEntry.copy(rating = -1)
                    )
                } else {
                    //latest recent comments has no blog entries in recent action, so most likely not low rated
                    require(index == blogEntries.size)
                    blogEntries.add(blogEntry)
                }
            }
            if (id !in usedIds) {
                usedIds.add(id)
                val curIndex = blogEntries.indexOfFirst { it.id == id }
                index = max(index, curIndex + 1)
            }
        }
        return Pair(blogEntries, comments)
    }

    fun addToFollowList(userInfo: CodeforcesUserInfo, context: Context) {
        viewModelScope.launch {
            context.followListDao.addNewUser(
                userInfo = userInfo,
                context = context
            )
        }
    }

    fun addToFollowList(handle: String, context: Context) {
        viewModelScope.launch {
            context.followListDao.addNewUser(
                handle = handle,
                context = context
            )
        }
    }

    private val followLoadingStatus = MutableStateFlow(LoadingStatus.PENDING)
    fun flowOfFollowUpdateLoadingStatus(): StateFlow<LoadingStatus> = followLoadingStatus
    fun updateFollowUsersInfo(context: Context) {
        if (!followLoadingStatus.compareAndSet(LoadingStatus.PENDING, LoadingStatus.LOADING)) return
        viewModelScope.launch {
            context.followListDao.updateUsersInfo(context)
            followLoadingStatus.value = LoadingStatus.PENDING
        }
    }

    //TODO: no mutableState
    var blogLoadingStatus by mutableStateOf(LoadingStatus.PENDING)
    val blogEntriesState = mutableStateOf(emptyList<CodeforcesBlogEntry>())
    fun loadBlog(handle: String, context: Context) {
        require(blogLoadingStatus != LoadingStatus.LOADING)
        blogEntriesState.value = emptyList()
        viewModelScope.launch {
            blogLoadingStatus = LoadingStatus.LOADING
            val (result, colorTag) = awaitPair(
                context = Dispatchers.IO,
                blockFirst = { context.followListDao.getAndReloadBlogEntries(handle, context) },
                blockSecond = { CodeforcesUtils.getRealColorTag(handle) }
            )
            if (result == null) {
                blogLoadingStatus = LoadingStatus.FAILED
            } else {
                blogLoadingStatus = LoadingStatus.PENDING
                blogEntriesState.value = result.map {
                    it.copy(
                        title = CodeforcesUtils.extractTitle(it),
                        authorColorTag = colorTag
                    )
                }
            }
        }
    }

}