package com.demich.cps.community.codeforces

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import com.demich.cps.community.codeforces.CodeforcesCommunityController.RecentPageType
import com.demich.cps.platforms.api.codeforces.CodeforcesUrls
import com.demich.cps.platforms.utils.codeforces.CodeforcesRecentFeedBlogEntry
import com.demich.cps.platforms.utils.codeforces.CodeforcesWebComment
import com.demich.cps.profiles.managers.toHandleSpan
import com.demich.cps.ui.BackHandler
import com.demich.cps.ui.CPSIcons
import com.demich.cps.utils.context

@Composable
fun CodeforcesCommunityRecentPage(
    controller: CodeforcesCommunityController
) {
    val context = context
    val recent by controller.flowOfRecent(context).collectAsState()
    val grouped by remember {
        derivedStateOf {
            recent.grouped()
        }
    }

    val saveableStateHolder = rememberSaveableStateHolder()

    CodeforcesReloadablePage(controller = controller, tab = RECENT) {
        when (val type = controller.recentPageType) {
            is RecentPageType.BlogEntryRecentComments -> {
                RecentCommentsInBlogEntry(
                    recentComments = { recent.commentsOf(blogEntry = type.blogEntry) },
                    isTabVisible = { controller.isTabVisible(tab = RECENT) },
                    onClose = { controller.recentPageType = RecentPageType.RecentFeed },
                    modifier = Modifier.fillMaxSize()
                )
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
                    val uriHandler = LocalUriHandler.current
                    RecentBlogEntriesPage(
                        recent = { grouped },
                        modifier = Modifier.fillMaxSize(),
                        onBrowseComment = { blogEntry, comment ->
                            uriHandler.openUri(CodeforcesUrls.comment(blogEntryId = blogEntry.id, commentId = comment.id))
                        },
                        onBrowseBlogEntry = {
                            uriHandler.openUri(CodeforcesUrls.blogEntry(it.id))
                        },
                        onOpenComments = { controller.recentPageType = RecentPageType.BlogEntryRecentComments(it) }
                    )
                }
            }
        }
    }
}


@Composable
private fun RecentBlogEntriesPage(
    recent: () -> List<CodeforcesRecentCommentsOfBlogEntry>,
    modifier: Modifier = Modifier,
    onBrowseComment: (CodeforcesRecentFeedBlogEntry, CodeforcesWebComment) -> Unit,
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
    recentComments: () -> CodeforcesRecentCommentsOfBlogEntry,
    isTabVisible: () -> Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(
        enabled = isTabVisible,
        onBackPressed = onClose
    ) {
        RecentCommentsInBlogEntry(
            recentComments = recentComments(),
            modifier = modifier
        )
    }
}

@Composable
private fun RecentCommentsInBlogEntry(
    recentComments: CodeforcesRecentCommentsOfBlogEntry,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        val (blogEntry, comments) = recentComments
        RecentBlogEntry(
            title = blogEntry.title,
            authorHandle = blogEntry.author.toHandleSpan(),
            commentators = AnnotatedString(comments.size.toString()),
            isLowRated = false,
            modifier = Modifier.recentBlogEntryPaddings()
        )
        Divider()
        CodeforcesComments(
            comments = { comments },
            showTitle = false,
            modifier = Modifier.fillMaxSize()
        )
    }
}