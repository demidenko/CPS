package com.example.test3.news.codeforces

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.test3.CodeforcesTitle
import com.example.test3.news.codeforces.adapters.CodeforcesBlogEntriesAdapter
import com.example.test3.utils.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CodeforcesNewsViewModel: ViewModel() {


    private class DataLoader<T>(
        init: T,
        var ignoreLoad: Boolean = false
    ) {
        private val dataFlow: MutableStateFlow<T> = MutableStateFlow(init)
        fun getDataFlow(): StateFlow<T> = dataFlow.asStateFlow()

        val loadingState: MutableStateFlow<LoadingState> = MutableStateFlow(LoadingState.PENDING)

        suspend fun load(getData: suspend () -> T?) {
            if(ignoreLoad) return
            loadingState.value = LoadingState.LOADING
            val data = getData()
            if(data == null) loadingState.value = LoadingState.FAILED
            else {
                dataFlow.value = data
                loadingState.value = LoadingState.PENDING
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


    private val mainBlogEntries = DataLoader<List<CodeforcesBlogEntriesAdapter.BlogEntryInfo>>(emptyList())
    fun flowOfMainBlogEntries() = mainBlogEntries.getDataFlow()

    private val topBlogEntries = DataLoader<List<CodeforcesBlogEntriesAdapter.BlogEntryInfo>>(emptyList())
    fun flowOfTopBlogEntries() = topBlogEntries.getDataFlow()

    private var topComments = DataLoader<List<CodeforcesRecentAction>>(emptyList(), ignoreLoad = true)
    fun flowOfTopComments() = topComments.apply { ignoreLoad = false }.getDataFlow()

    private val recentActions = DataLoader<Pair<List<CodeforcesBlogEntry>,List<CodeforcesRecentAction>>>(Pair(emptyList(), emptyList()))
    fun flowOfRecentActions() = recentActions.getDataFlow()

    fun reload(title: CodeforcesTitle, lang: String) {
        viewModelScope.launch {
            when(title){
                CodeforcesTitle.MAIN -> mainBlogEntries.load { loadBlogEntriesPage("/", lang) }
                CodeforcesTitle.TOP -> {
                    topBlogEntries.load { loadBlogEntriesPage("/top", lang) }
                    topComments.load { loadCommentsPage("/topComments?days=2", lang) }
                }
                CodeforcesTitle.RECENT -> recentActions.load { loadRecentActionsPage(lang) }
                else -> return@launch
            }
        }
    }


    private suspend fun loadBlogEntriesPage(page: String, lang: String): List<CodeforcesBlogEntriesAdapter.BlogEntryInfo>? {
        val s = CodeforcesAPI.getPageSource(page, lang) ?: return null
        return CodeforcesUtils.parseBlogEntriesPage(s).takeIf { it.isNotEmpty() }
    }

    private suspend fun loadCommentsPage(page: String, lang: String): List<CodeforcesRecentAction>? {
        val s = CodeforcesAPI.getPageSource(page, lang) ?: return null
        return CodeforcesUtils.parseCommentsPage(s).takeIf { it.isNotEmpty() }
    }

    private suspend fun loadRecentActionsPage(lang: String): Pair<List<CodeforcesBlogEntry>,List<CodeforcesRecentAction>>? {
        val s = CodeforcesAPI.getPageSource("/recent-actions", lang) ?: return null
        val blogEntries = CodeforcesUtils.parseRecentBlogEntriesPage(s)
        val comments = CodeforcesUtils.parseCommentsPage(s)
        if(blogEntries.isEmpty() || comments.isEmpty()) return null
        return Pair(blogEntries, comments)
    }
}