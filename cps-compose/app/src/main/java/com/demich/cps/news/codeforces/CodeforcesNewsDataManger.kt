package com.demich.cps.news.codeforces

import android.content.Context
import com.demich.cps.platforms.api.CodeforcesBlogEntry
import com.demich.cps.platforms.api.CodeforcesRecentAction
import com.demich.cps.utils.LoadingStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface CodeforcesNewsDataManger {
    fun flowOfLoadingStatus(): Flow<LoadingStatus>
    fun flowOfLoadingStatus(title: CodeforcesTitle): Flow<LoadingStatus>

    fun reloadAll(context: Context)
    fun reload(title: CodeforcesTitle, context: Context)

    fun flowOfMainBlogEntries(context: Context): StateFlow<List<CodeforcesBlogEntry>>
    fun flowOfTopBlogEntries(context: Context): StateFlow<List<CodeforcesBlogEntry>>
    fun flowOfTopComments(context: Context): StateFlow<List<CodeforcesRecentAction>>
    fun flowOfRecentActions(context: Context): StateFlow<Pair<List<CodeforcesBlogEntry>, List<CodeforcesRecentAction>>>

    fun addToFollowList(handle: String, context: Context)
    fun updateFollowUsersInfo(context: Context)
}