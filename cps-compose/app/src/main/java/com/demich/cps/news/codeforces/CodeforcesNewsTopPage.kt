package com.demich.cps.news.codeforces

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberCollect

@Composable
fun CodeforcesNewsTopPage(
    controller: CodeforcesNewsController
) {
    val saveableStateHolder = rememberSaveableStateHolder()

    CodeforcesReloadablePage(controller = controller, title = CodeforcesTitle.TOP) {
        if (controller.topShowComments) {
            saveableStateHolder.SaveableStateProvider(key = true) {
                CodeforcesNewsTopComments(controller = controller)
            }
        } else {
            saveableStateHolder.SaveableStateProvider(key = false) {
                CodeforcesNewsTopBlogEntries(controller = controller)
            }
        }
    }
}

@Composable
private fun CodeforcesNewsTopBlogEntries(
    controller: CodeforcesNewsController
) {
    val context = context
    val blogEntriesState = rememberCollect { controller.flowOfTopBlogEntries(context) }
    CodeforcesBlogEntries(
        blogEntriesController = rememberCodeforcesBlogEntriesController(blogEntriesState = blogEntriesState),
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun CodeforcesNewsTopComments(
    controller: CodeforcesNewsController
) {
    val context = context
    val commentsState = rememberCollect { controller.flowOfTopComments(context) }
    CodeforcesComments(
        commentsState = commentsState,
        modifier = Modifier.fillMaxSize()
    )
}