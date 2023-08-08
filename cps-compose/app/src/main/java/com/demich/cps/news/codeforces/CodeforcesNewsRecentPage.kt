package com.demich.cps.news.codeforces

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import com.demich.cps.ui.CPSIcons
import com.demich.cps.utils.context
import com.demich.cps.utils.openUrlInBrowser
import com.demich.cps.utils.rememberCollect
import com.demich.cps.utils.rememberWith
import com.demich.cps.platforms.api.CodeforcesApi
import com.demich.cps.platforms.api.CodeforcesBlogEntry
import com.demich.cps.platforms.api.CodeforcesRecentAction

@Composable
fun CodeforcesNewsRecentPage(
    controller: CodeforcesNewsController
) {
    val context = context

    val recentActions by rememberCollect { controller.flowOfRecentActions(context) }

    val saveableStateHolder = rememberSaveableStateHolder()

    CodeforcesReloadablePage(controller = controller, title = CodeforcesTitle.RECENT) {
        val blogEntry = controller.recentFilterByBlogEntry
        if (blogEntry != null) {
            saveableStateHolder.SaveableStateProvider(key = blogEntry.id) {
                RecentCommentsInBlogEntry(
                    controller = controller,
                    comments = { recentActions.second },
                    blogEntry = recentActions.first.firstOrNull { it.id == blogEntry.id } ?: blogEntry,
                    onBackPressed = {
                        saveableStateHolder.removeState(blogEntry.id)
                        controller.recentFilterByBlogEntry = null
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else
        if (controller.recentShowComments) {
            saveableStateHolder.SaveableStateProvider(key = true) {
                CodeforcesComments(
                    comments = { recentActions.second },
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            saveableStateHolder.SaveableStateProvider(key = false) {
                RecentBlogEntriesPage(
                    controller = controller,
                    recentActions = { recentActions },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}


@Composable
private fun RecentBlogEntriesPage(
    controller: CodeforcesNewsController,
    recentActions: () -> Pair<List<CodeforcesBlogEntry>, List<CodeforcesRecentAction>>,
    modifier: Modifier = Modifier
) {
    val context = context

    fun openBlogEntry(blogEntry: CodeforcesBlogEntry) {
        context.openUrlInBrowser(CodeforcesApi.urls.blogEntry(blogEntry.id))
    }

    CodeforcesRecentBlogEntries(
        recentActions = recentActions,
        modifier = modifier,
        onOpenBlogEntry = ::openBlogEntry,
    ) { blogEntry, comments ->
        CPSDropdownMenuItem(title = "Open recent comment", icon = CPSIcons.OpenInBrowser) {
            context.openUrlInBrowser(
                CodeforcesApi.urls.comment(
                blogEntryId = blogEntry.id,
                commentId = comments.first().id
            ))
        }
        CPSDropdownMenuItem(title = "Open blog entry", icon = CPSIcons.OpenInBrowser) {
            openBlogEntry(blogEntry)
        }
        CPSDropdownMenuItem(title = "Show recent comments", icon = CPSIcons.Comments) {
            controller.recentFilterByBlogEntry = blogEntry
        }
    }
}

@Composable
private fun RecentCommentsInBlogEntry(
    controller: CodeforcesNewsController,
    comments: () -> List<CodeforcesRecentAction>,
    blogEntry: CodeforcesBlogEntry,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier
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
        modifier = modifier
    )

    BackHandler(
        enabled = controller.isTabVisible(CodeforcesTitle.RECENT),
        onBack = onBackPressed
    )
}