package com.demich.cps.news.codeforces

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.demich.cps.news.follow.CodeforcesBlogEntriesFollowAddable
import com.demich.cps.utils.context
import com.demich.cps.utils.mapToSet
import kotlinx.coroutines.flow.map

@Composable
fun CodeforcesNewsLostPage(controller: CodeforcesNewsController) {
    val context = context
    val newEntriesItem = remember { CodeforcesNewEntriesDataStore(context).lostNewEntries }

    val listState = rememberLazyListState()

    val blogEntriesController = rememberCodeforcesBlogEntriesController(
        tab = CodeforcesTitle.LOST,
        blogEntriesFlow = controller.flowOfLostBlogEntries(context),
        newEntriesItem = newEntriesItem,
        listState = listState,
        controller = controller
    )

    val topIdsState = remember {
        controller.flowOfTopBlogEntries(context).map { blogEntries ->
            blogEntries.mapToSet { it.id }
        }
    }.collectAsState(initial = emptySet())

    CodeforcesBlogEntriesFollowAddable(
        controller = controller,
        blogEntriesController = blogEntriesController,
        topBlogEntriesIdsState = topIdsState,
        lazyListState = listState,
        modifier = Modifier.fillMaxSize()
    )
}