package com.example.test3.news.codeforces

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.test3.CodeforcesTitle
import com.example.test3.news.codeforces.adapters.CodeforcesBlogEntriesAdapter
import com.example.test3.utils.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CodeforcesNewsViewModel: ViewModel() {

    private val states = mutableMapOf<CodeforcesTitle, MutableStateFlow<LoadingState>>()
    private fun pageLoadingState(title: CodeforcesTitle) = states.getOrPut(title) { MutableStateFlow(LoadingState.PENDING) }
    fun getPageLoadingStateFlow(title: CodeforcesTitle) = pageLoadingState(title).asStateFlow()


    private val blogEntriesMain = MutableStateFlow<List<CodeforcesBlogEntriesAdapter.BlogEntryInfo>>(emptyList())
    fun getBlogEntriesMain() = blogEntriesMain.asStateFlow()

    private val blogEntriesTop = MutableStateFlow<List<CodeforcesBlogEntriesAdapter.BlogEntryInfo>>(emptyList())
    fun getBlogEntriesTop() = blogEntriesTop.asStateFlow()

    private val recentActions = MutableStateFlow<Pair<List<CodeforcesBlogEntry>,List<CodeforcesRecentAction>>>(Pair(emptyList(), emptyList()))
    fun getRecentActionsData() = recentActions.asStateFlow()

    fun reload(title: CodeforcesTitle, lang: String) {
        viewModelScope.launch {
            when(title){
                CodeforcesTitle.MAIN -> proceedLoading(title, blogEntriesMain) { loadBlogEntriesPage("/", lang) }
                CodeforcesTitle.TOP -> proceedLoading(title, blogEntriesTop) { loadBlogEntriesPage("/top", lang) }
                CodeforcesTitle.RECENT -> proceedLoading(title, recentActions) { loadRecentActionsPage(lang) }
                else -> return@launch
            }
        }
    }

    private inline fun<reified T> proceedLoading(
        title: CodeforcesTitle,
        flowOut: MutableStateFlow<T>,
        getData: () -> T?
    ) {
        val loadingState = pageLoadingState(title)
        loadingState.value = LoadingState.LOADING
        val data = getData()
        if(data == null) loadingState.value = LoadingState.FAILED
        else {
            flowOut.value = data
            loadingState.value = LoadingState.PENDING
        }
    }


    private suspend fun loadBlogEntriesPage(page: String, lang: String): List<CodeforcesBlogEntriesAdapter.BlogEntryInfo>? {
        val s = CodeforcesAPI.getPageSource(page, lang) ?: return null
        return CodeforcesUtils.parseBlogEntriesPage(s).takeIf { it.isNotEmpty() }
    }

    private suspend fun loadRecentActionsPage(lang: String): Pair<List<CodeforcesBlogEntry>,List<CodeforcesRecentAction>>? {
        val s = CodeforcesAPI.getPageSource("/recent-actions", lang) ?: return null
        val blogEntries = CodeforcesUtils.parseRecentBlogEntriesPage(s)
        val comments = CodeforcesUtils.parseCommentsPage(s)
        if(blogEntries.isEmpty() || comments.isEmpty()) return null
        return Pair(blogEntries, comments)
    }
}