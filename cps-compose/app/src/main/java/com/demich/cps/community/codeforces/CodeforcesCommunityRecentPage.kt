package com.demich.cps.community.codeforces

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import com.demich.cps.accounts.managers.toHandleSpan
import com.demich.cps.community.codeforces.CodeforcesCommunityController.RecentPageType
import com.demich.cps.community.codeforces.CodeforcesTitle.RECENT
import com.demich.cps.platforms.api.codeforces.CodeforcesUrls
import com.demich.cps.platforms.api.codeforces.models.CodeforcesComment
import com.demich.cps.platforms.api.codeforces.models.CodeforcesRecentAction
import com.demich.cps.platforms.utils.codeforces.CodeforcesRecentFeed
import com.demich.cps.platforms.utils.codeforces.CodeforcesRecentFeedBlogEntry
import com.demich.cps.ui.BackHandler
import com.demich.cps.ui.CPSIcons
import com.demich.cps.utils.collectAsState
import com.demich.cps.utils.context
import com.demich.cps.utils.openUrlInBrowser

@Composable
fun CodeforcesCommunityRecentPage(
    controller: CodeforcesCommunityController
) {
    val context = context
    val recent by collectAsState { controller.flowOfRecent(context) }

    val saveableStateHolder = rememberSaveableStateHolder()

    CodeforcesReloadablePage(controller = controller, title = RECENT) {
        when (val type = controller.recentPageType) {
            is RecentPageType.BlogEntryRecentComments -> {
                val blogEntry = type.blogEntry
                BackHandler(
                    enabled = { controller.isTabVisible(RECENT) },
                    onBackPressed = { controller.recentPageType = RecentPageType.RecentFeed }
                ) {
                    RecentCommentsInBlogEntry(
                        comments = { recent.comments },
                        blogEntry = recent.blogEntries.firstOrNull { it.id == blogEntry.id } ?: blogEntry,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            RecentPageType.RecentComments -> {
                saveableStateHolder.SaveableStateProvider(key = true) {
                    CodeforcesComments(
                        comments = { recent.comments },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            RecentPageType.RecentFeed -> {
                saveableStateHolder.SaveableStateProvider(key = false) {
                    RecentBlogEntriesPage(
                        recent = { recent },
                        modifier = Modifier.fillMaxSize(),
                        onBrowseComment = { blogEntry, comment ->
                            context.openUrlInBrowser(CodeforcesUrls.comment(
                                blogEntryId = blogEntry.id,
                                commentId = comment.id
                            ))
                        },
                        onBrowseBlogEntry = { context.openUrlInBrowser(CodeforcesUrls.blogEntry(it.id)) },
                        onOpenComments = { controller.recentPageType = RecentPageType.BlogEntryRecentComments(it) }
                    )
                }
            }
        }
    }
}


@Composable
private fun RecentBlogEntriesPage(
    recent: () -> CodeforcesRecentFeed,
    modifier: Modifier = Modifier,
    onBrowseComment: (CodeforcesRecentFeedBlogEntry, CodeforcesComment) -> Unit,
    onBrowseBlogEntry: (CodeforcesRecentFeedBlogEntry) -> Unit,
    onOpenComments: (CodeforcesRecentFeedBlogEntry) -> Unit
) {
    CodeforcesRecentBlogEntries(
        recent = recent,
        modifier = modifier,
        onBrowseBlogEntry = onBrowseBlogEntry,
    ) { (blogEntry, comments) ->
        CPSDropdownMenuItem(title = "Open recent comment", icon = CPSIcons.OpenInBrowser) {
            onBrowseComment(blogEntry, comments.first())
        }
        CPSDropdownMenuItem(title = "Open blog entry", icon = CPSIcons.OpenInBrowser) {
            onBrowseBlogEntry(blogEntry)
        }
        CPSDropdownMenuItem(title = "Show recent comments", icon = CPSIcons.Comments) {
            onOpenComments(blogEntry)
        }
    }
}

@Composable
private fun RecentCommentsInBlogEntry(
    comments: () -> List<CodeforcesRecentAction>,
    blogEntry: CodeforcesRecentFeedBlogEntry,
    modifier: Modifier = Modifier
) {
    val id = blogEntry.id
    val filteredComments by remember(key1 = id, key2 = comments) {
        derivedStateOf {
            comments().filter { it.blogEntry?.id == id }
        }
    }

    Column(modifier = modifier) {
        RecentBlogEntry(
            title = blogEntry.title,
            authorHandle = blogEntry.author.toHandleSpan(),
            commentators = AnnotatedString(filteredComments.size.toString()),
            isLowRated = false,
            modifier = Modifier.recentBlogEntryPaddings()
        )
        Divider()
        CodeforcesComments(
            comments = { filteredComments },
            showTitle = false,
            modifier = Modifier.fillMaxSize()
        )
    }
}