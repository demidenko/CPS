package com.demich.cps.news.codeforces

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.demich.cps.utils.*
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

@Composable
fun CodeforcesNewsMainPage(
    controller: CodeforcesNewsController
) {
    val context = context
    val loadingStatus by controller.rememberLoadingStatusState(CodeforcesTitle.MAIN)
    SwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing = loadingStatus == LoadingStatus.LOADING),
        onRefresh = { controller.reload(title = CodeforcesTitle.MAIN, context = context) },
    ) {
        CodeforcesNewsMainList(controller = controller)
    }
}

@Composable
private fun CodeforcesNewsMainList(
    controller: CodeforcesNewsController
) {
    val context = context
    val newEntriesDataStore = remember { CodeforcesNewEntriesDataStore(context) }

    CodeforcesBlogEntries(
        blogEntriesController = rememberCodeforcesBlogEntriesController(
            blogEntriesFlow = controller.flowOfMainBlogEntries(context),
            newEntriesItem = newEntriesDataStore.mainNewEntries
        ),
        modifier = Modifier.fillMaxSize()
    )
}

