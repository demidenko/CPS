package com.demich.cps.community.codeforces

import com.demich.cps.platforms.utils.codeforces.CodeforcesRecentFeed
import com.demich.cps.platforms.utils.codeforces.CodeforcesWebBlogEntry
import com.demich.cps.platforms.utils.codeforces.CodeforcesWebComment
import com.demich.cps.utils.LoadingStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface CodeforcesCommunityBackgroundLoader {
    fun flowOfLoadingStatus(): Flow<LoadingStatus>
    fun flowOfLoadingStatus(tab: CodeforcesTab): Flow<LoadingStatus>

    fun reload(tab: CodeforcesTab)

    fun flowOfMainBlogEntries(): StateFlow<List<CodeforcesWebBlogEntry>?>
    fun flowOfTopBlogEntries(): StateFlow<List<CodeforcesWebBlogEntry>?>
    fun flowOfTopComments(): StateFlow<List<CodeforcesWebComment>?>
    fun flowOfRecent(): StateFlow<CodeforcesRecentFeed?>
}

fun CodeforcesCommunityBackgroundLoader.reload(tabs: Collection<CodeforcesTab>) {
    tabs.forEach { reload(tab = it) }
}