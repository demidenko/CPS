package com.demich.cps.community.codeforces

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.demich.cps.community.follow.CodeforcesBlogEntriesFollowAddable
import com.demich.cps.utils.context

@Composable
fun CodeforcesCommunityMainPage(
    controller: CodeforcesCommunityController,
    newEntriesState: NewEntriesState
) {
    CodeforcesReloadablePage(controller = controller, title = CodeforcesTitle.MAIN) {
        CodeforcesCommunityMainList(controller, newEntriesState)
    }
}

@Composable
private fun CodeforcesCommunityMainList(
    controller: CodeforcesCommunityController,
    newEntriesState: NewEntriesState
) {
    val context = context
    val listState = rememberLazyListState()

    val blogEntriesState = rememberCodeforcesBlogEntriesState(
        blogEntriesFlow = controller.flowOfMainBlogEntries(context),
        isTabVisible = { controller.isTabVisible(CodeforcesTitle.MAIN) },
        listState = listState,
        newEntriesState = newEntriesState,
        showNewEntries = true
    )

    CodeforcesBlogEntriesFollowAddable(
        controller = controller,
        blogEntriesState = blogEntriesState,
        lazyListState = listState,
        modifier = Modifier.fillMaxSize()
    )
}

