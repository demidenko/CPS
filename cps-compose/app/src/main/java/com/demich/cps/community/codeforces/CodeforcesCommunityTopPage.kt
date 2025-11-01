package com.demich.cps.community.codeforces

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import com.demich.cps.community.codeforces.CodeforcesCommunityController.TopPageType.BlogEntries
import com.demich.cps.community.codeforces.CodeforcesCommunityController.TopPageType.Comments
import com.demich.cps.community.codeforces.CodeforcesTitle.TOP
import com.demich.cps.community.follow.CodeforcesBlogEntriesFollowAddable
import com.demich.cps.utils.collectAsState
import com.demich.cps.utils.context

@Composable
fun CodeforcesCommunityTopPage(
    controller: CodeforcesCommunityController,
    newEntriesState: NewEntriesState
) {
    val saveableStateHolder = rememberSaveableStateHolder()

    CodeforcesReloadablePage(controller = controller, title = TOP) {
        when (val key = controller.topPageType) {
            BlogEntries -> {
                saveableStateHolder.SaveableStateProvider(key = key) {
                    CodeforcesCommunityTopBlogEntries(controller, newEntriesState)
                }
            }
            Comments -> {
                saveableStateHolder.SaveableStateProvider(key = key) {
                    CodeforcesCommunityTopComments(controller = controller)
                }
            }
        }
    }
}

@Composable
private fun CodeforcesCommunityTopBlogEntries(
    controller: CodeforcesCommunityController,
    newEntriesState: NewEntriesState
) {
    val context = context
    val listState = rememberLazyListState()

    val blogEntriesState = rememberCodeforcesBlogEntriesState(
        blogEntriesFlow = controller.flowOfTopBlogEntries(context),
        isTabVisible = { controller.isTabVisible(tab = TOP) },
        listState = listState,
        newEntriesState = newEntriesState,
        showNewEntries = false
    )
    CodeforcesBlogEntriesFollowAddable(
        controller = controller,
        blogEntriesState = blogEntriesState,
        lazyListState = listState,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun CodeforcesCommunityTopComments(
    controller: CodeforcesCommunityController
) {
    val context = context
    val comments by collectAsState { controller.flowOfTopComments(context) }
    CodeforcesComments(
        comments = { comments },
        modifier = Modifier.fillMaxSize()
    )
}