package com.demich.cps.news.codeforces

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.demich.cps.ui.CPSIcons
import com.demich.cps.utils.codeforces.CodeforcesApi
import com.demich.cps.utils.codeforces.CodeforcesBlogEntry
import com.demich.cps.utils.codeforces.CodeforcesRecentAction
import com.demich.cps.utils.context
import com.demich.cps.utils.openUrlInBrowser
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
            RecentBlogEntriesPage(
                controller = controller,
                recentActionsState = recentActionsState
            )
        }
    }
}


@Composable
private fun RecentBlogEntriesPage(
    controller: CodeforcesNewsController,
    recentActionsState: State<Pair<List<CodeforcesBlogEntry>, List<CodeforcesRecentAction>>>,
) {
    val context = context
    CodeforcesRecentBlogEntries(
        recentActionsState = recentActionsState,
        modifier = Modifier.fillMaxSize()
    ) { (blogEntry, comments) ->
        CPSDropdownMenuItem(title = "Open recent comment", icon = CPSIcons.OpenInBrowser) {
            context.openUrlInBrowser(CodeforcesApi.urls.comment(
                blogEntryId = blogEntry.id,
                commentId = comments.first().id
            ))
        }
        CPSDropdownMenuItem(title = "Open blog entry", icon = CPSIcons.OpenInBrowser) {
            context.openUrlInBrowser(CodeforcesApi.urls.blogEntry(blogEntry.id))
        }
        CPSDropdownMenuItem(title = "Show recent comments", icon = CPSIcons.Comments) {
            //TODO
        }
    }
}