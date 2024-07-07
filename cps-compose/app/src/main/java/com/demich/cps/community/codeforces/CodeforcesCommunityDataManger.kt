package com.demich.cps.community.codeforces

import android.content.Context
import com.demich.cps.platforms.api.CodeforcesBlogEntry
import com.demich.cps.platforms.api.CodeforcesRecentAction
import com.demich.cps.platforms.utils.codeforces.CodeforcesRecent
import com.demich.cps.utils.LoadingStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface CodeforcesCommunityDataManger {
    fun flowOfLoadingStatus(): Flow<LoadingStatus>
    fun flowOfLoadingStatus(title: CodeforcesTitle): Flow<LoadingStatus>

    fun reloadAll(context: Context)
    fun reload(title: CodeforcesTitle, context: Context)

    fun flowOfMainBlogEntries(context: Context): StateFlow<List<CodeforcesBlogEntry>>
    fun flowOfTopBlogEntries(context: Context): StateFlow<List<CodeforcesBlogEntry>>
    fun flowOfTopComments(context: Context): StateFlow<List<CodeforcesRecentAction>>
    fun flowOfRecent(context: Context): StateFlow<CodeforcesRecent>

    fun addToFollowList(handle: String, context: Context)
    fun updateFollowUsersInfo(context: Context)
}