package com.demich.cps.community.codeforces

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import com.demich.cps.community.follow.CodeforcesBlogEntriesFollowAddable
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberCollect

@Composable
fun CodeforcesCommunityTopPage(
    controller: CodeforcesCommunityController,
    newEntriesState: NewEntriesState
) {
    val saveableStateHolder = rememberSaveableStateHolder()

    CodeforcesReloadablePage(controller = controller, title = CodeforcesTitle.TOP) {
        when (val key = controller.topType) {
            CodeforcesCommunityController.TopType.BlogEntries -> {
                saveableStateHolder.SaveableStateProvider(key = key) {
                    CodeforcesCommunityTopBlogEntries(controller, newEntriesState)
                }
            }
            CodeforcesCommunityController.TopType.Comments -> {
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
        isTabVisible = { controller.isTabVisible(CodeforcesTitle.TOP) },
        listState = listState,
        newEntriesState = newEntriesState,
        showNewEntries = false
    )
    CodeforcesBlogEntriesFollowAddable(
        controller = controller,
        blogEntriesState = blogEntriesState,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun CodeforcesCommunityTopComments(
    controller: CodeforcesCommunityController
) {
    val context = context
    val comments by rememberCollect { controller.flowOfTopComments(context) }
    CodeforcesComments(
        comments = { comments },
        modifier = Modifier.fillMaxSize()
    )
}