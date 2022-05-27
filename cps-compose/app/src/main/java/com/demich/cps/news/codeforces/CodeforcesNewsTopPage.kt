package com.demich.cps.news.codeforces

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import com.demich.cps.utils.LoadingStatus
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberCollect
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

@Composable
fun CodeforcesNewsTopPage(
    controller: CodeforcesNewsController
) {
    val context = context
    val loadingStatus by controller.rememberLoadingStatusState(CodeforcesTitle.TOP)

    val saveableStateHolder = rememberSaveableStateHolder()

    SwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing = loadingStatus == LoadingStatus.LOADING),
        onRefresh = { controller.reload(title = CodeforcesTitle.TOP, context = context) },
    ) {
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
        blogEntriesState = blogEntriesState,
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