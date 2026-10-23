package com.demich.cps.community.codeforces

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.demich.cps.community.follow.CodeforcesBlogEntriesFollowAddable

@Composable
fun CodeforcesCommunityMainPage(
    backgroundLoader: CodeforcesCommunityBackgroundLoader,
    pagerState: CodeforcesCommunityPagerState,
    newEntriesState: NewEntriesState
) {
    CodeforcesReloadablePage(backgroundLoader = backgroundLoader, tab = MAIN) {
        CodeforcesCommunityMainList(
            backgroundLoader = backgroundLoader,
            pagerState = pagerState,
            newEntriesState = newEntriesState
        )
    }
}

@Composable
private fun CodeforcesCommunityMainList(
    backgroundLoader: CodeforcesCommunityBackgroundLoader,
    pagerState: CodeforcesCommunityPagerState,
    newEntriesState: NewEntriesState
) {
    val listState = rememberLazyListState()

    val codeforcesNewEntriesState = rememberCodeforcesNewEntriesState(
        isListVisible = { pagerState.isTabVisible(tab = MAIN) },
        listState = listState,
        newEntriesState = newEntriesState,
        showNewEntries = true
    )

    val blogEntries by backgroundLoader.flowOfMainBlogEntries().collectAsState()

    CodeforcesBlogEntriesFollowAddable(
        blogEntries = blogEntries,
        newEntriesState = codeforcesNewEntriesState,
        lazyListState = listState,
        modifier = Modifier.fillMaxSize()
    )
}

