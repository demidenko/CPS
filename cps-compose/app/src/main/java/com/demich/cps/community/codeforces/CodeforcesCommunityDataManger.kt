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
    fun flowOfLoadingStatus(tab: CodeforcesTab): Flow<LoadingStatus>

    fun reload(tabs: List<CodeforcesTab>, context: Context)

    fun flowOfMainBlogEntries(context: Context): StateFlow<List<CodeforcesWebBlogEntry>>
    fun flowOfTopBlogEntries(context: Context): StateFlow<List<CodeforcesWebBlogEntry>>
    fun flowOfTopComments(context: Context): StateFlow<List<CodeforcesWebComment>>
    fun flowOfRecent(context: Context): StateFlow<CodeforcesRecentFeed>

    fun updateFollowUsersInfo(context: Context)
}

fun CodeforcesCommunityDataManger.reload(tab: CodeforcesTab, context: Context) {
    reload(tabs = listOf(tab), context = context)
}