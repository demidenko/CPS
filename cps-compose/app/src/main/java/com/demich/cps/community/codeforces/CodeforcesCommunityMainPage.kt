package com.demich.cps.community.codeforces

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.demich.cps.community.follow.CodeforcesBlogEntriesFollowAddable
import com.demich.cps.utils.context

@Composable
fun CodeforcesCommunityMainPage(
    controller: CodeforcesCommunityController
) {
    CodeforcesReloadablePage(controller = controller, title = CodeforcesTitle.MAIN) {
        CodeforcesCommunityMainList(controller = controller)
    }
}

@Composable
private fun CodeforcesCommunityMainList(
    controller: CodeforcesCommunityController
) {
    val context = context
    val newEntriesItem = remember { CodeforcesNewEntriesDataStore(context).mainNewEntries }

    val listState = rememberLazyListState()

    val blogEntriesController = rememberCodeforcesBlogEntriesController(
        tab = CodeforcesTitle.MAIN,
        blogEntriesFlow = controller.flowOfMainBlogEntries(context),
        newEntriesItem = newEntriesItem,
        listState = listState,
        controller = controller
    )

    CodeforcesBlogEntriesFollowAddable(
        controller = controller,
        blogEntriesController = blogEntriesController,
        lazyListState = listState,
        modifier = Modifier.fillMaxSize()
    )
}

