package com.demich.cps.news.codeforces

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.ui.CPSDropdownMenuScope
import com.demich.cps.ui.ContentWithCPSDropdownMenu
import com.demich.cps.ui.itemsNotEmpty
import com.demich.cps.utils.codeforces.CodeforcesBlogEntry
import com.demich.cps.utils.codeforces.CodeforcesComment
import com.demich.cps.utils.codeforces.CodeforcesRecentAction

@Composable
fun CodeforcesRecentBlogEntries(
    recentActionsState: State<Pair<List<CodeforcesBlogEntry>, List<CodeforcesRecentAction>>>,
    modifier: Modifier = Modifier,
    menuBuilder: @Composable CPSDropdownMenuScope.(CodeforcesRecentBlogEntry) -> Unit
) {
    val recent = remember(recentActionsState.value) {
        val (blogEntries, comments) = recentActionsState.value
        val commentsGrouped = comments.groupBy { it.blogEntry?.id }
        blogEntries.map { blogEntry ->
            CodeforcesRecentBlogEntry(
                blogEntry = blogEntry,
                comments = commentsGrouped[blogEntry.id]
                    ?.map { it.comment }
                    ?.distinctBy { it.commentatorHandle }
                    ?: emptyList()
            )
        }
    }

    var showMenuForBlogEntryId: Int? by remember { mutableStateOf(null) }
    LazyColumn(modifier = modifier) {
        itemsNotEmpty(items = recent) {
            ContentWithCPSDropdownMenu(
                modifier = Modifier
                    .clickable(enabled = it.comments.isNotEmpty()) {
                        showMenuForBlogEntryId = it.blogEntry.id
                    }
                    .fillMaxWidth()
                    .padding(start = 3.dp, end = 3.dp, bottom = 2.dp),
                expanded = it.blogEntry.id == showMenuForBlogEntryId,
                menuAlignment = Alignment.CenterStart,
                onDismissRequest = { showMenuForBlogEntryId = null },
                menuBuilder = { menuBuilder(it) },
                content = { RecentBlogEntry(recentBlogEntryData = it) }
            )
            Divider()
        }
    }
}

@Immutable
data class CodeforcesRecentBlogEntry(
    val blogEntry: CodeforcesBlogEntry,
    val comments: List<CodeforcesComment>
)

@Composable
private fun RecentBlogEntry(
    recentBlogEntryData: CodeforcesRecentBlogEntry,
    modifier: Modifier = Modifier
) {
    val manager = LocalCodeforcesAccountManager.current
    RecentBlogEntry(
        title = recentBlogEntryData.blogEntry.title,
        authorHandle = manager.makeHandleSpan(
            handle = recentBlogEntryData.blogEntry.authorHandle,
            tag = recentBlogEntryData.blogEntry.authorColorTag
        ),
        commentators = buildAnnotatedString {
            recentBlogEntryData.comments.forEachIndexed { index, comment ->
                if (index > 0) append(", ")
                append(manager.makeHandleSpan(handle = comment.commentatorHandle, tag = comment.commentatorHandleColorTag))
            }
        },
        modifier = modifier
    )
}

@Composable
private fun RecentBlogEntry(
    title: String,
    authorHandle: AnnotatedString,
    commentators: AnnotatedString,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        BlogEntryTitleWithArrow(
            title = title,
            fontSize = 16.sp,
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