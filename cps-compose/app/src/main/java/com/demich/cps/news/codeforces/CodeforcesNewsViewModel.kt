package com.demich.cps.news.codeforces

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demich.cps.news.settingsNews
import com.demich.cps.utils.LoadingStatus
import com.demich.cps.utils.codeforces.CodeforcesApi
import com.demich.cps.utils.codeforces.CodeforcesBlogEntry
import com.demich.cps.utils.codeforces.CodeforcesLocale
import com.demich.cps.utils.codeforces.CodeforcesUtils
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
                    load(locale = context.settingsNews.codeforcesLocale())
                }
            }
            return dataFlow.asStateFlow()
        }

        val loadingStatusState = mutableStateOf(LoadingStatus.PENDING)

        fun load(locale: CodeforcesLocale) {
            if (!touched) return
            require(loadingStatusState.value != LoadingStatus.LOADING)
            viewModelScope.launch {
                loadingStatusState.value = LoadingStatus.LOADING
                val data = getData(locale)
                if(data == null) loadingStatusState.value = LoadingStatus.FAILED
                else {
                    dataFlow.value = data
                    loadingStatusState.value = LoadingStatus.PENDING
                }
            }
        }
    }

    fun pageLoadingStatusState(title: CodeforcesTitle): State<LoadingStatus> {
        return when (title) {
            CodeforcesTitle.MAIN -> mainBlogEntries.loadingStatusState
            CodeforcesTitle.TOP -> topBlogEntries.loadingStatusState
            else -> TODO()
        }
    }

    private val mainBlogEntries = DataLoader(emptyList()) { loadBlogEntriesPage("/", it) }
    fun flowOfMainBlogEntries(context: Context) = mainBlogEntries.getDataFlow(context)

    private val topBlogEntries = DataLoader(emptyList()) { loadBlogEntriesPage("/top", it) }
    fun flowOfTopBlogEntries(context: Context) = topBlogEntries.getDataFlow(context)

    fun reload(title: CodeforcesTitle, locale: CodeforcesLocale) {
        when(title) {
            CodeforcesTitle.MAIN -> mainBlogEntries.load(locale)
            CodeforcesTitle.TOP -> {
                topBlogEntries.load(locale)
                //topComments.load(locale)
            }
            //CodeforcesTitle.RECENT -> recentActions.load(locale)
            else -> return
        }
    }

    private suspend fun loadBlogEntriesPage(page: String, locale: CodeforcesLocale): List<CodeforcesBlogEntry>? {
        val s = CodeforcesApi.getPageSource(CodeforcesApi.urls.main + page, locale) ?: return null
        return CodeforcesUtils.extractBlogEntries(s).takeIf { it.isNotEmpty() }
    }
}