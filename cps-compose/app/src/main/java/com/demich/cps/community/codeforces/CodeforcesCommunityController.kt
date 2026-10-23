package com.demich.cps.community.codeforces

import android.content.Context
import androidx.compose.runtime.Stable


@Stable
class CodeforcesCommunityController(
    backgroundLoader: CodeforcesCommunityBackgroundLoader,
    val pagerState: CodeforcesCommunityPagerState
): CodeforcesCommunityBackgroundLoader by backgroundLoader {

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