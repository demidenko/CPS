package com.demich.cps.community.codeforces

import android.content.Context
import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


@Stable
class CodeforcesCommunityController(
    backgroundLoader: CodeforcesCommunityBackgroundLoader,
    val pagerState: CodeforcesCommunityPagerState
): CodeforcesCommunityBackgroundLoader by backgroundLoader {

}

fun CodeforcesCommunityBackgroundLoader.flowOfEntryIds(tab: CodeforcesTab, context: Context): Flow<List<Int>>? {
    val flow = when (tab) {
        MAIN -> flowOfMainBlogEntries(context)
        LOST -> flowOfLostBlogEntries(context)
        else -> return null
    }
    return flow.map { it?.map { it.id } ?: [] }
}

fun CodeforcesCommunityController.touchFlows(context: Context) {
    pagerState.visitedTabs.forEach { tab ->
        when (tab) {
            MAIN -> flowOfMainBlogEntries(context)
            TOP -> {
                when (pagerState.topPageType) {
                    BlogEntries -> flowOfTopBlogEntries(context)
                    Comments -> flowOfTopComments(context)
                }
            }
            RECENT -> flowOfRecent(context)
            else -> { }
        }
    }
}