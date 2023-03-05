package com.demich.cps.news.codeforces

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
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
import com.demich.cps.utils.rememberWith

@Composable
fun CodeforcesNewsRecentPage(
    controller: CodeforcesNewsController
) {
    val context = context

    val recentActions by rememberCollect { controller.flowOfRecentActions(context) }
    val comments by remember {
        derivedStateOf { recentActions.second }
    }

    val saveableStateHolder = rememberSaveableStateHolder()

    CodeforcesReloadablePage(controller = controller, title = CodeforcesTitle.RECENT) {
        val blogEntryId = controller.recentFilterByBlogEntryId
        if (blogEntryId != null) {
            saveableStateHolder.SaveableStateProvider(key = blogEntryId) {
                RecentCommentsInBlogEntry(
                    controller = controller,
                    comments = { comments },
                    //TODO: NoSuchElement crash
                    blogEntry = recentActions.first.first { it.id == blogEntryId },
                    onBackPressed = {
                        saveableStateHolder.removeState(blogEntryId)
                    }
                )
            }
        } else
        if (controller.recentShowComments) {
            saveableStateHolder.SaveableStateProvider(key = true) {
                CodeforcesComments(
                    comments = { comments },
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            saveableStateHolder.SaveableStateProvider(key = false) {
                RecentBlogEntriesPage(
                    controller = controller,
                    recentActions = { recentActions }
                )
            }
        }
    }
}


@Composable
private fun RecentBlogEntriesPage(
    controller: CodeforcesNewsController,
    recentActions: () -> Pair<List<CodeforcesBlogEntry>, List<CodeforcesRecentAction>>,
) {
    val context = context

    fun openBlogEntry(blogEntry: CodeforcesBlogEntry) {
        context.openUrlInBrowser(CodeforcesApi.urls.blogEntry(blogEntry.id))
    }

    CodeforcesRecentBlogEntries(
        recentActions = recentActions,
        modifier = Modifier.fillMaxSize(),
        onOpenBlogEntry = ::openBlogEntry,
    ) { blogEntry, comments ->
        CPSDropdownMenuItem(title = "Open recent comment", icon = CPSIcons.OpenInBrowser) {
            context.openUrlInBrowser(CodeforcesApi.urls.comment(
                blogEntryId = blogEntry.id,
                commentId = comments.first().id
            ))
        }
        CPSDropdownMenuItem(title = "Open blog entry", icon = CPSIcons.OpenInBrowser) {
            openBlogEntry(blogEntry)
        }
        CPSDropdownMenuItem(title = "Show recent comments", icon = CPSIcons.Comments) {
            controller.recentFilterByBlogEntryId = blogEntry.id
        }
    }
}

@Composable
private fun RecentCommentsInBlogEntry(
    controller: CodeforcesNewsController,
    comments: () -> List<CodeforcesRecentAction>,
    blogEntry: CodeforcesBlogEntry,
    onBackPressed: () -> Unit
) {
    val filteredComments by rememberWith(blogEntry) {
        derivedStateOf {
            comments().filter {
                it.blogEntry?.id == id
            }
        }
    }

    CodeforcesComments(
        comments = { filteredComments },
        showTitle = false,
        modifier = Modifier.fillMaxSize()
    )

    BackHandler(
        enabled = controller.isTabVisible(CodeforcesTitle.RECENT)
    ) {
        onBackPressed()
        controller.recentFilterByBlogEntryId = null
    }
}