package com.demich.cps.news.codeforces

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.demich.cps.utils.LoadingStatus
import com.demich.cps.utils.NewEntryType
import com.demich.cps.utils.context
import com.demich.cps.utils.visibleRange
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

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

    val listState = rememberLazyListState()

    val blogEntriesController = rememberCodeforcesBlogEntriesController(
        blogEntriesFlow = controller.flowOfMainBlogEntries(context),
        newEntriesItem = newEntriesDataStore.mainNewEntries
    )

    CodeforcesBlogEntries(
        blogEntriesController = blogEntriesController,
        lazyListState = listState,
        modifier = Modifier.fillMaxSize()
    )

    LaunchedEffect(controller, listState) {
        snapshotFlow<List<Int>> {
            if (!controller.isTabVisible(CodeforcesTitle.MAIN)) return@snapshotFlow emptyList()
            val blogEntries = blogEntriesController.blogEntries
            listState.visibleRange(0.75f).map { blogEntries[it].id }
        }.onEach {
            newEntriesDataStore.mainNewEntries.markAtLeast(
                ids = it.map(Int::toString),
                type = NewEntryType.SEEN
            )
        }.collect()
    }
}

