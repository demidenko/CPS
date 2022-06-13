package com.demich.cps.news.codeforces

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.demich.cps.news.follow.CodeforcesBlogEntriesFollowAddable
import com.demich.cps.utils.context

@Composable
fun CodeforcesNewsLostPage(controller: CodeforcesNewsController) {
    val context = context

    val blogEntriesFlow = remember {
        controller.flowOfLostBlogEntries(context)
    }

    val newEntriesItem = remember {
        CodeforcesNewEntriesDataStore(context).lostNewEntries
    }

    val listState = rememberLazyListState()

    CodeforcesBlogEntriesFollowAddable(
        controller = controller,
        blogEntriesController = rememberCodeforcesBlogEntriesController(
            tab = CodeforcesTitle.LOST,
            controller = controller,
            blogEntriesFlow = blogEntriesFlow,
            newEntriesItem = newEntriesItem,
            listState = listState
        ),
        lazyListState = listState,
        modifier = Modifier.fillMaxSize()
    )
}