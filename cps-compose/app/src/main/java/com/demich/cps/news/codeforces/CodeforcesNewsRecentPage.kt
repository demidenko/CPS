package com.demich.cps.news.codeforces

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.demich.cps.utils.LoadingStatus
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberCollect
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

@Composable
fun CodeforcesNewsRecentPage(
    controller: CodeforcesNewsController
) {
    val context = context
    val loadingStatus by controller.rememberLoadingStatusState(CodeforcesTitle.RECENT)
    val recentActionsState = rememberCollect { controller.flowOfRecentActions(context) }
    val commentsState = remember(recentActionsState.value) {
        mutableStateOf(recentActionsState.value.second)
    }

    SwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing = loadingStatus == LoadingStatus.LOADING),
        onRefresh = { controller.reload(title = CodeforcesTitle.RECENT, context = context) },
    ) {
        if (controller.recentShowComments) {
            CodeforcesComments(
                commentsState = commentsState,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            CodeforcesRecentBlogEntries(
                recentActionsState = recentActionsState,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}