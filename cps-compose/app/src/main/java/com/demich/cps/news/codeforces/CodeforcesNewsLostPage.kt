package com.demich.cps.news.codeforces

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.demich.cps.news.follow.CodeforcesBlogEntriesFollowAddable
import com.demich.cps.ui.theme.cpsColors
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
        lazyListState = listState,
        modifier = Modifier.fillMaxSize(),
        label = {
            if (topIdsState.value.contains(it.id)) TopLabel()
        }
    )
}

@Composable
private fun TopLabel() {
    Text(
        text = "TOP",
        color = cpsColors.contentAdditional,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold
    )
}