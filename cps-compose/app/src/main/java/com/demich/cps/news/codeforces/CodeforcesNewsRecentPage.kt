package com.demich.cps.news.codeforces

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
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
    val commentsState = remember {
        derivedStateOf { recentActionsState.value.second }
    }

    val saveableStateHolder = rememberSaveableStateHolder()

    CodeforcesReloadablePage(controller = controller, title = CodeforcesTitle.RECENT) {
        val blogEntryId = controller.recentFilterByBlogEntryId
        if (blogEntryId != null) {
            saveableStateHolder.SaveableStateProvider(key = blogEntryId) {
                RecentCommentsInBlogEntry(
                    controller = controller,
                    commentsState = commentsState,
                    blogEntry = recentActionsState.value.first.first { it.id == blogEntryId },
                    onBackPressed = saveableStateHolder::removeState
                )
            }
        } else
        if (controller.recentShowComments) {
            saveableStateHolder.SaveableStateProvider(key = true) {
                CodeforcesComments(
                    commentsState = commentsState,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            saveableStateHolder.SaveableStateProvider(key = false) {
                RecentBlogEntriesPage(
                    controller = controller,
                    recentActionsState = recentActionsState
                )
            }
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
    ) { blogEntry, comments ->
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
            controller.recentFilterByBlogEntryId = blogEntry.id
        }
    }
}

@Composable
private fun RecentCommentsInBlogEntry(
    controller: CodeforcesNewsController,
    commentsState: State<List<CodeforcesRecentAction>>,
    blogEntry: CodeforcesBlogEntry,
    onBackPressed: (Int) -> Unit
) {
    val filteredCommentsState = remember(blogEntry) {
        derivedStateOf {
            commentsState.value.filter {
                it.blogEntry?.id == blogEntry.id
            }
        }
    }

    CodeforcesComments(
        commentsState = filteredCommentsState,
        showTitle = false,
        modifier = Modifier.fillMaxSize()
    )

    BackHandler(
        enabled = controller.isTabVisible(CodeforcesTitle.RECENT)
    ) {
        controller.recentFilterByBlogEntryId?.let(onBackPressed)
        controller.recentFilterByBlogEntryId = null
    }
}