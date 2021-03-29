package com.example.test3.news.codeforces

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.test3.CodeforcesTitle
import com.example.test3.news.codeforces.adapters.CodeforcesBlogEntriesAdapter
import com.example.test3.utils.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CodeforcesNewsViewModel: ViewModel() {

    private val states = mutableMapOf<CodeforcesTitle, MutableLiveData<LoadingState>>()
    private fun pageLoadingState(title: CodeforcesTitle) = states.getOrPut(title) { MutableLiveData(LoadingState.PENDING) }
    fun getPageLoadingStateLiveData(title: CodeforcesTitle): LiveData<LoadingState> = pageLoadingState(title)


    private val blogEntriesMain = MutableStateFlow<List<CodeforcesBlogEntriesAdapter.BlogEntryInfo>>(emptyList())
    fun getBlogEntriesMain() = blogEntriesMain.asStateFlow()

    private val blogEntriesTop = MutableStateFlow<List<CodeforcesBlogEntriesAdapter.BlogEntryInfo>>(emptyList())
    fun getBlogEntriesTop() = blogEntriesTop.asStateFlow()

    private val recentActions = MutableStateFlow<Pair<List<CodeforcesBlogEntry>,List<CodeforcesRecentAction>>>(Pair(emptyList(), emptyList()))
    fun getRecentActionsData() = recentActions.asStateFlow()

    fun reload(title: CodeforcesTitle, lang: String) {
        if(title == CodeforcesTitle.LOST) return
        viewModelScope.launch {
            val loadingState = pageLoadingState(title)
            loadingState.value = LoadingState.LOADING

            when(title){
                CodeforcesTitle.MAIN -> {
                    val blogEntries = loadBlogEntriesPage("/", lang)
                    if (blogEntries == null) loadingState.value = LoadingState.FAILED
                    else {
                        blogEntriesMain.value = blogEntries
                        loadingState.value = LoadingState.PENDING
                    }
                }
                CodeforcesTitle.TOP -> {
                    val blogEntries = loadBlogEntriesPage("/top", lang)
                    if (blogEntries == null) loadingState.value = LoadingState.FAILED
                    else {
                        blogEntriesTop.value = blogEntries
                        loadingState.value = LoadingState.PENDING
                    }
                }
                CodeforcesTitle.RECENT -> {
                    val recentActionData = loadRecentActionsPage(lang)
                    if(recentActionData == null) loadingState.value = LoadingState.FAILED
                    else {
                        recentActions.value = recentActionData
                        loadingState.value = LoadingState.PENDING
                    }
                }
            }
        }
    }


    private suspend fun loadBlogEntriesPage(page: String, lang: String): List<CodeforcesBlogEntriesAdapter.BlogEntryInfo>? {
        val s = CodeforcesAPI.getPageSource(page, lang) ?: return null
        return CodeforcesUtils.parseBlogEntriesPage(s).takeIf { it.isNotEmpty() }
    }

    private suspend fun loadRecentActionsPage(lang: String): Pair<List<CodeforcesBlogEntry>,List<CodeforcesRecentAction>>? {
        val s = CodeforcesAPI.getPageSource("/recent-actions", lang) ?: return null
        return CodeforcesUtils.parseRecentActionsPage(s).takeIf { it.first.isNotEmpty() }
    }
}