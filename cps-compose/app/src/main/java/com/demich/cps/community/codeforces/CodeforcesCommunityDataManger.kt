package com.demich.cps.community.codeforces

import android.content.Context
import com.demich.cps.platforms.utils.codeforces.CodeforcesRecentFeed
import com.demich.cps.platforms.utils.codeforces.CodeforcesWebBlogEntry
import com.demich.cps.platforms.utils.codeforces.CodeforcesWebComment
import com.demich.cps.utils.LoadingStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface CodeforcesCommunityDataManger {
    fun flowOfLoadingStatus(): Flow<LoadingStatus>
    fun flowOfLoadingStatus(title: CodeforcesTitle): Flow<LoadingStatus>

    fun reload(titles: List<CodeforcesTitle>, context: Context)

    fun flowOfMainBlogEntries(context: Context): StateFlow<List<CodeforcesWebBlogEntry>>
    fun flowOfTopBlogEntries(context: Context): StateFlow<List<CodeforcesWebBlogEntry>>
    fun flowOfTopComments(context: Context): StateFlow<List<CodeforcesWebComment>>
    fun flowOfRecent(context: Context): StateFlow<CodeforcesRecentFeed>

    fun addToFollowList(handle: String, context: Context)
    fun updateFollowUsersInfo(context: Context)
}

fun CodeforcesCommunityDataManger.reload(title: CodeforcesTitle, context: Context) {
    reload(titles = listOf(title), context = context)
}