package com.demich.cps.news.codeforces

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberCollect

@Composable
fun CodeforcesNewsRecentPage(
    controller: CodeforcesNewsController
) {
    val context = context
    val recentActionsState = rememberCollect { controller.flowOfRecentActions(context) }
    val commentsState = remember(recentActionsState.value) {
        mutableStateOf(recentActionsState.value.second)
    }

    CodeforcesReloadablePage(controller = controller, title = CodeforcesTitle.RECENT) {
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