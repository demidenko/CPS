package com.demich.cps.community.codeforces

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.demich.cps.community.follow.CodeforcesBlogEntriesFollowAddable
import com.demich.cps.utils.context

@Composable
fun CodeforcesCommunityTopPage(
    controller: CodeforcesCommunityController,
    newEntriesState: NewEntriesState
) {
    val saveableStateHolder = rememberSaveableStateHolder()

    CodeforcesReloadablePage(controller = controller, tab = TOP) {
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
        isTabVisible = { controller.isTabVisible(tab = TOP) },
        listState = listState,
        newEntriesState = newEntriesState,
        showNewEntries = false
    )

    val blogEntries by controller.flowOfTopBlogEntries(context).collectAsStateWithLifecycle()

    CodeforcesBlogEntriesFollowAddable(
        blogEntries = { blogEntries },
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
    val comments by controller.flowOfTopComments(context).collectAsState()
    CodeforcesComments(
        comments = { comments },
        modifier = Modifier.fillMaxSize()
    )
}