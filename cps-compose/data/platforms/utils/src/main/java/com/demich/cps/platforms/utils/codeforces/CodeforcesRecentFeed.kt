package com.demich.cps.platforms.utils.codeforces

data class CodeforcesRecentFeed(
    val blogEntries: List<CodeforcesRecentFeedBlogEntry>,
    val comments: List<CodeforcesWebComment>
)

data class CodeforcesRecentFeedBlogEntry(
    val id: Int,
    val title: String,
    val author: CodeforcesHandle,
    val isLowRated: Boolean
)