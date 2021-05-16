package com.example.test3.news.codeforces

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.test3.CodeforcesLocale
import com.example.test3.CodeforcesTitle
import com.example.test3.NewsFragment
import com.example.test3.account_manager.CodeforcesAccountManager
import com.example.test3.account_manager.CodeforcesUserInfo
import com.example.test3.room.CodeforcesUserBlog
import com.example.test3.room.getFollowDao
import com.example.test3.utils.*
import com.example.test3.workers.CodeforcesNewsLostRecentWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CodeforcesNewsViewModel: ViewModel() {


    private inner class DataLoader<T>(
        init: T,
        val getData: suspend (CodeforcesLocale) -> T?
    ) {
        private val dataFlow: MutableStateFlow<T> = MutableStateFlow(init)

        private var touched = false
        fun getDataFlow(context: Context): StateFlow<T> {
            if (!touched) {
                touched = true
                viewModelScope.launch {
                    load(NewsFragment.getCodeforcesContentLanguage(context))
                }
            }
            return dataFlow.asStateFlow()
        }

        val loadingState: MutableStateFlow<LoadingState> = MutableStateFlow(LoadingState.PENDING)

        fun load(locale: CodeforcesLocale) {
            if(!touched) return
            loadingState.value = LoadingState.LOADING
            viewModelScope.launch {
                val data = getData(locale)
                if(data == null) loadingState.value = LoadingState.FAILED
                else {
                    dataFlow.value = data
                    loadingState.value = LoadingState.PENDING
                }
            }
        }
    }

    fun getPageLoadingStateFlow(title: CodeforcesTitle): Flow<LoadingState> {
        return when(title){
            CodeforcesTitle.MAIN -> mainBlogEntries.loadingState
            CodeforcesTitle.RECENT -> recentActions.loadingState
            CodeforcesTitle.TOP -> {
                LoadingState.combineLoadingStateFlows(listOf(
                    topBlogEntries.loadingState,
                    topComments.loadingState
                ))
            }
            else -> throw IllegalArgumentException("$title does not support reload")
        }
    }


    private val mainBlogEntries = DataLoader(emptyList()) { loadBlogEntriesPage("/", it) }
    fun flowOfMainBlogEntries(context: Context) = mainBlogEntries.getDataFlow(context)

    private val topBlogEntries = DataLoader(emptyList()) { loadBlogEntriesPage("/top", it) }
    fun flowOfTopBlogEntries(context: Context) = topBlogEntries.getDataFlow(context)

    private val topComments = DataLoader(emptyList()) { loadCommentsPage("/topComments?days=2", it) }
    fun flowOfTopComments(context: Context) = topComments.getDataFlow(context)

    private val recentActions = DataLoader(Pair(emptyList(), emptyList())) { loadRecentActionsPage(it) }
    fun flowOfRecentActions(context: Context) = recentActions.getDataFlow(context)

    fun reload(title: CodeforcesTitle, locale: CodeforcesLocale) {
        when(title){
            CodeforcesTitle.MAIN -> mainBlogEntries.load(locale)
            CodeforcesTitle.TOP -> {
                topBlogEntries.load(locale)
                topComments.load(locale)
            }
            CodeforcesTitle.RECENT -> recentActions.load(locale)
            else -> return
        }
    }

    private suspend fun loadBlogEntriesPage(page: String, locale: CodeforcesLocale): List<CodeforcesBlogEntry>? {
        val s = CodeforcesAPI.getPageSource(page, locale) ?: return null
        return CodeforcesUtils.parseBlogEntriesPage(s).takeIf { it.isNotEmpty() }
    }

    private suspend fun loadCommentsPage(page: String, locale: CodeforcesLocale): List<CodeforcesRecentAction>? {
        val s = CodeforcesAPI.getPageSource(page, locale) ?: return null
        return CodeforcesUtils.parseCommentsPage(s).takeIf { it.isNotEmpty() }
    }

    private suspend fun loadRecentActionsPage(locale: CodeforcesLocale): Pair<List<CodeforcesBlogEntry>,List<CodeforcesRecentAction>>? {
        val s = CodeforcesAPI.getPageSource("/recent-actions", locale) ?: return null
        val blogEntries = CodeforcesUtils.parseRecentBlogEntriesPage(s)
        val comments = CodeforcesUtils.parseCommentsPage(s)
        if(blogEntries.isEmpty() || comments.isEmpty()) return null
        return Pair(blogEntries, comments)
    }

    private val updateLostInfoProgress = MutableStateFlow<Pair<Int,Int>?>(null)
    fun getUpdateLostInfoProgress() = updateLostInfoProgress.asStateFlow()
    fun updateLostInfo(context: Context) {
        viewModelScope.launch {
            CodeforcesNewsLostRecentWorker.updateInfo(context, updateLostInfoProgress)
        }
    }

    suspend fun addToFollowList(info: CodeforcesUserInfo, context: Context): Boolean {
        val dao = getFollowDao(context)
        if(dao.getUserBlog(info.handle)!=null) return false
        viewModelScope.launch {
            dao.insert(
                CodeforcesUserBlog(
                    handle = info.handle,
                    blogEntries = null,
                    userInfo = info
                )
            )
            val locale = NewsFragment.getCodeforcesContentLanguage(context)
            val blogEntries = CodeforcesAPI.getUserBlogEntries(info.handle,locale)?.result?.map { it.id }
            dao.setBlogEntries(info.handle, blogEntries)
        }
        return true
    }

    suspend fun addToFollowList(handle: String, context: Context): Boolean {
        val dao = getFollowDao(context)
        if(dao.getUserBlog(handle)!=null) return false
        viewModelScope.launch {
            val manager = CodeforcesAccountManager(context)
            dao.insert(
                CodeforcesUserBlog(
                    handle = handle,
                    blogEntries = null,
                    userInfo = manager.emptyInfo()
                )
            )
            launch {
                val locale = NewsFragment.getCodeforcesContentLanguage(context)
                val blogEntries = CodeforcesAPI.getUserBlogEntries(handle,locale)?.result?.map { it.id }
                dao.setBlogEntries(handle, blogEntries)
            }
            launch {
                dao.setUserInfo(handle, manager.loadInfo(handle))
            }
        }
        return true
    }

    fun removeFromFollowList(handle: String, context: Context) {
        viewModelScope.launch {
            getFollowDao(context).remove(handle)
        }
    }

}