package com.demich.cps.community.codeforces

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.demich.kotlin_stdlib_boost.swap
import kotlinx.serialization.Serializable

class CodeforcesCommunityPagerState(
    val tabs: List<CodeforcesTab>,
    pagerData: CodeforcesCommunityPagerData
): PagerState(currentPage = tabs.indexOf(pagerData.selectedTab).let { if (it != -1) it else 0 }) {

    override val pageCount: Int
        get() = tabs.size

    val currentTab: CodeforcesTab
        get() = tabs[currentPage]

    // relies that tabs are always fixed!
    // not saved/restored!
    //TODO: incorrect?????
    val visitedTabs by tabs.toMutableList().let { list ->
        derivedStateOf {
            val pos = list.indexOf(currentTab)
            for (i in pos downTo 1) list.swap(i, i-1)
            list.toList()
        }
    }

    fun isTabVisible(tab: CodeforcesTab) = tab == currentTab && !isScrollInProgress

    suspend fun scrollTo(tab: CodeforcesTab) = animateScrollToPage(page = tabs.indexOf(tab))


    var topPageType by mutableStateOf(pagerData.topPageType)
    var recentPageType by mutableStateOf(pagerData.recentPageType)

    enum class TopPageType {
        BlogEntries, Comments
    }

    @Serializable
    sealed interface RecentPageType {
        @Serializable
        data object RecentFeed : RecentPageType
        @Serializable
        data object RecentComments : RecentPageType
        @Serializable
        data class BlogEntryRecentComments(val blogEntryId: Int) : RecentPageType
    }
}

@Serializable
data class CodeforcesCommunityPagerData(
    val selectedTab: CodeforcesTab,
    val topPageType: CodeforcesCommunityPagerState.TopPageType,
    val recentPageType: CodeforcesCommunityPagerState.RecentPageType
)