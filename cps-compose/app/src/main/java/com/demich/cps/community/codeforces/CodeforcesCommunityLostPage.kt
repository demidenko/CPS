package com.demich.cps.community.codeforces

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.demich.cps.community.follow.CodeforcesBlogEntriesFollowAddable
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.context
import com.demich.kotlin_stdlib_boost.mapToSet
import kotlinx.coroutines.flow.map

@Composable
fun CodeforcesCommunityLostPage(
    controller: CodeforcesCommunityController,
    newEntriesState: NewEntriesState
) {
    val context = context
    val listState = rememberLazyListState()

    val blogEntriesState = rememberCodeforcesBlogEntriesState(
        blogEntriesFlow = controller.flowOfLostBlogEntries(context),
        isTabVisible = { controller.isTabVisible(CodeforcesTitle.LOST) },
        listState = listState,
        newEntriesState = newEntriesState,
        showNewEntries = true
    )

    val topIds by remember {
        controller.flowOfTopBlogEntries(context).map { blogEntries ->
            blogEntries.mapToSet { it.id }
        }
    }.collectAsStateWithLifecycle(initialValue = emptySet())

    CodeforcesBlogEntriesFollowAddable(
        controller = controller,
        blogEntriesState = blogEntriesState,
        lazyListState = listState,
        modifier = Modifier.fillMaxSize(),
        scrollBarEnabled = true,
        label = {
            if (it.id in topIds) TopLabel()
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