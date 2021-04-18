package com.example.test3.news.codeforces

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.test3.CodeforcesLocale
import com.example.test3.CodeforcesTitle
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
        var ignoreLoad: Boolean = false
    ) {
        private val dataFlow: MutableStateFlow<T> = MutableStateFlow(init)
        fun getDataFlow(): StateFlow<T> = dataFlow.asStateFlow()

        val loadingState: MutableStateFlow<LoadingState> = MutableStateFlow(LoadingState.PENDING)

        fun load(getData: suspend () -> T?) {
            if(ignoreLoad) return
            loadingState.value = LoadingState.LOADING
            viewModelScope.launch {
                val data = getData()
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


    private val mainBlogEntries = DataLoader<List<CodeforcesBlogEntry>>(emptyList())
    fun flowOfMainBlogEntries() = mainBlogEntries.getDataFlow()

    private val topBlogEntries = DataLoader<List<CodeforcesBlogEntry>>(emptyList())
    fun flowOfTopBlogEntries() = topBlogEntries.getDataFlow()

    private val topComments = DataLoader<List<CodeforcesRecentAction>>(emptyList(), ignoreLoad = true)
    fun flowOfTopComments() = topComments.apply { ignoreLoad = false }.getDataFlow()

    private val recentActions = DataLoader<Pair<List<CodeforcesBlogEntry>,List<CodeforcesRecentAction>>>(Pair(emptyList(), emptyList()))
    fun flowOfRecentActions() = recentActions.getDataFlow()

    fun reload(title: CodeforcesTitle, locale: CodeforcesLocale) {
        when(title){
            CodeforcesTitle.MAIN -> mainBlogEntries.load { loadBlogEntriesPage("/", locale) }
            CodeforcesTitle.TOP -> {
                topBlogEntries.load { loadBlogEntriesPage("/top", locale) }
                reloadTopComments(locale)
            }
            CodeforcesTitle.RECENT -> recentActions.load { loadRecentActionsPage(locale) }
            else -> return
        }
    }

    fun reloadTopComments(locale: CodeforcesLocale) {
        topComments.load { loadCommentsPage("/topComments?days=2", locale) }
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
}