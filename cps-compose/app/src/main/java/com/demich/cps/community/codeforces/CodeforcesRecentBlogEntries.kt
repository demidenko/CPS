package com.demich.cps.community.codeforces

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.accounts.managers.toHandleSpan
import com.demich.cps.platforms.api.codeforces.models.CodeforcesComment
import com.demich.cps.platforms.utils.codeforces.CodeforcesRecentFeed
import com.demich.cps.platforms.utils.codeforces.CodeforcesRecentFeedBlogEntry
import com.demich.cps.platforms.utils.codeforces.commentator
import com.demich.cps.ui.CPSDropdownMenuScope
import com.demich.cps.ui.ContentWithCPSDropdownMenu
import com.demich.cps.ui.lazylist.LazyColumnOfData
import com.demich.cps.ui.theme.cpsColors

@Composable
internal fun CodeforcesRecentBlogEntries(
    recent: () -> CodeforcesRecentFeed,
    modifier: Modifier = Modifier,
    onBrowseBlogEntry: (CodeforcesRecentFeedBlogEntry) -> Unit,
    menuBuilder: @Composable CPSDropdownMenuScope.(RecentBlogEntryData) -> Unit
) {
    val recentData by remember(recent) {
        derivedStateOf {
            recent().makeRecentBlogEntries()
        }
    }

    var showMenuForBlogEntryId: Int? by remember { mutableStateOf(null) }
    LazyColumnOfData(
        modifier = modifier,
        items = { recentData },
        scrollBarEnabled = false
    ) {
        ContentWithCPSDropdownMenu(
            modifier = Modifier
                .clickable {
                    if (it.comments.isEmpty()) onBrowseBlogEntry(it.blogEntry)
                    else showMenuForBlogEntryId = it.blogEntry.id
                }
                .fillMaxWidth()
                .recentBlogEntryPaddings(),
            expanded = it.blogEntry.id == showMenuForBlogEntryId,
            menuAlignment = Alignment.CenterStart,
            onDismissRequest = { showMenuForBlogEntryId = null },
            menuBuilder = { menuBuilder(it) },
            content = { RecentBlogEntry(recentBlogEntryData = it) }
        )
        Divider()
    }
}

internal fun Modifier.recentBlogEntryPaddings() =
    padding(start = 3.dp, end = 3.dp, bottom = 2.dp, top = 1.dp)

@Immutable
internal data class RecentBlogEntryData(
    val blogEntry: CodeforcesRecentFeedBlogEntry,
    val comments: List<CodeforcesComment>, //only first comment per each commentator
) {
    val isLowRated: Boolean get() = blogEntry.isLowRated
}

private fun CodeforcesRecentFeed.makeRecentBlogEntries(): List<RecentBlogEntryData> {
    val commentsGrouped = comments.groupBy { it.blogEntry?.id }
    return blogEntries.map { blogEntry ->
        RecentBlogEntryData(
            blogEntry = blogEntry,
            comments = commentsGrouped[blogEntry.id]
                ?.mapNotNull { it.comment } //TODO: nulls skipped
                ?.distinctBy { it.commentatorHandle }
                ?: emptyList()
        )
    }
}

@Composable
private fun RecentBlogEntry(
    recentBlogEntryData: RecentBlogEntryData,
    modifier: Modifier = Modifier
) {
    RecentBlogEntry(
        title = recentBlogEntryData.blogEntry.title,
        authorHandle = recentBlogEntryData.blogEntry.author.toHandleSpan(),
        commentators = buildAnnotatedString {
            recentBlogEntryData.comments.forEachIndexed { index, comment ->
                if (index > 0) append(", ")
                append(comment.commentator.toHandleSpan())
            }
        },
        isLowRated = recentBlogEntryData.isLowRated,
        modifier = modifier
    )
}

@Composable
internal fun RecentBlogEntry(
    title: String,
    authorHandle: AnnotatedString,
    commentators: AnnotatedString,
    isLowRated: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        BlogEntryTitleWithArrow(
            title = title,
            titleColor = if (isLowRated) cpsColors.contentAdditional else cpsColors.content,
            singleLine = false,
            modifier = Modifier.fillMaxWidth()
        )
        RecentBlogEntryFooter(
            authorHandle = authorHandle,
            commentators = commentators,
            fontSize = 13.sp,
            iconSize = 12.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun RecentBlogEntryFooter(
    authorHandle: AnnotatedString,
    commentators: AnnotatedString,
    modifier: Modifier = Modifier,
    fontSize: TextUnit,
    iconSize: TextUnit
) {
    Row(modifier = modifier.fillMaxWidth()) {
        Text(
            text = authorHandle,
            fontSize = fontSize,
            maxLines = 1,
        )
        if (commentators.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp)
                    .fillMaxWidth()
            ) {
                CommentsRow(
                    text = commentators,
                    fontSize = fontSize,
                    iconSize = iconSize,
                    spaceSize = 1.dp,
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }
        }
    }
}