package com.demich.cps.platforms.utils.codeforces

import com.demich.cps.platforms.api.codeforces.models.CodeforcesRecentAction
import kotlinx.serialization.Serializable

data class CodeforcesRecentFeed(
    val blogEntries: List<CodeforcesRecentFeedBlogEntry>,
    val comments: List<CodeforcesRecentAction>
)

@Serializable
data class CodeforcesRecentFeedBlogEntry(
    val id: Int,
    val title: String,
    val author: CodeforcesHandle,
    val isLowRated: Boolean
)