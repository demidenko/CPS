package com.demich.cps.news.codeforces

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import com.demich.cps.utils.NewEntryType
import com.demich.cps.utils.context
import com.demich.cps.utils.visibleRange
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

@Composable
fun CodeforcesNewsMainPage(
    controller: CodeforcesNewsController
) {
    CodeforcesReloadablePage(controller = controller, title = CodeforcesTitle.MAIN) {
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
            if (blogEntries.isEmpty()) return@snapshotFlow emptyList()
            listState.visibleRange(0.75f).map { blogEntries[it].id }
        }.onEach {
            newEntriesDataStore.mainNewEntries.markAtLeast(
                ids = it.map(Int::toString),
                type = NewEntryType.SEEN
            )
        }.collect()
    }
}

