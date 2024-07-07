package com.demich.cps.community.codeforces

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import com.demich.cps.community.follow.CodeforcesBlogEntriesFollowAddable
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberCollect

@Composable
fun CodeforcesCommunityTopPage(
    controller: CodeforcesCommunityController
) {
    val saveableStateHolder = rememberSaveableStateHolder()

    CodeforcesReloadablePage(controller = controller, title = CodeforcesTitle.TOP) {
        if (controller.topShowComments) {
            saveableStateHolder.SaveableStateProvider(key = true) {
                CodeforcesCommunityTopComments(controller = controller)
            }
        } else {
            saveableStateHolder.SaveableStateProvider(key = false) {
                CodeforcesCommunityTopBlogEntries(controller = controller)
            }
        }
    }
}

@Composable
private fun CodeforcesCommunityTopBlogEntries(
    controller: CodeforcesCommunityController
) {
    val context = context
    val blogEntries by rememberCollect { controller.flowOfTopBlogEntries(context) }
    CodeforcesBlogEntriesFollowAddable(
        controller = controller,
        blogEntriesController = rememberCodeforcesBlogEntriesController { blogEntries },
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