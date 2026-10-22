package com.demich.cps.community.codeforces

import kotlinx.serialization.Serializable

class CodeforcesCommunityPagerState {

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