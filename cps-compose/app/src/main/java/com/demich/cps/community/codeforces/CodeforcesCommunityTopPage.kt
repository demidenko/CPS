package com.demich.cps.community.codeforces

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import com.demich.cps.community.follow.CodeforcesBlogEntriesFollowAddable

@Composable
fun CodeforcesCommunityTopPage(
    backgroundLoader: CodeforcesCommunityBackgroundLoader,
    pagerState: CodeforcesCommunityPagerState,
    newEntriesState: NewEntriesState
) {
    val saveableStateHolder = rememberSaveableStateHolder()

    CodeforcesReloadablePage(backgroundLoader = backgroundLoader, tab = TOP) {
        when (val key = pagerState.topPageType) {
            BlogEntries -> {
                saveableStateHolder.SaveableStateProvider(key = key) {
                    CodeforcesCommunityTopBlogEntries(
                        backgroundLoader = backgroundLoader,
                        pagerState = pagerState,
                        newEntriesState = newEntriesState
                    )
                }
            }
            Comments -> {
                saveableStateHolder.SaveableStateProvider(key = key) {
                    CodeforcesCommunityTopComments(backgroundLoader = backgroundLoader)
                }
            }
        }
    }
}

@Composable
private fun CodeforcesCommunityTopBlogEntries(
    backgroundLoader: CodeforcesCommunityBackgroundLoader,
    pagerState: CodeforcesCommunityPagerState,
    newEntriesState: NewEntriesState
) {
    val listState = rememberLazyListState()

    val codeforcesNewEntriesState = rememberCodeforcesNewEntriesState(
        isListVisible = { pagerState.isTabVisible(tab = TOP) },
        listState = listState,
        newEntriesState = newEntriesState,
        showNewEntries = false
    )

    val blogEntries by backgroundLoader.flowOfTopBlogEntries().collectAsState()

    CodeforcesBlogEntriesFollowAddable(
        blogEntries = blogEntries,
        newEntriesState = codeforcesNewEntriesState,
        lazyListState = listState,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun CodeforcesCommunityTopComments(
    backgroundLoader: CodeforcesCommunityBackgroundLoader
) {
    val comments by backgroundLoader.flowOfTopComments().collectAsState()
    CodeforcesComments(
        comments = comments,
        modifier = Modifier.fillMaxSize()
    )
}