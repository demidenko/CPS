package com.demich.cps.news.codeforces

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.demich.cps.utils.codeforces.CodeforcesBlogEntry
import com.demich.cps.utils.codeforces.CodeforcesRecentAction
import com.demich.cps.utils.codeforces.CodeforcesUtils

@Composable
fun CodeforcesRecentBlogEntries(
    recentActionsState: State<Pair<List<CodeforcesBlogEntry>, List<CodeforcesRecentAction>>>,
    modifier: Modifier = Modifier
) {
    val recent = remember(recentActionsState.value) {
        val (blogEntries, comments) = recentActionsState.value
        val commentsGrouped = comments.groupBy { it.blogEntry?.id }
        blogEntries.map { blogEntry ->
            RecentBlogEntryData(
                blogEntry = blogEntry,
                commentatorsHandles = commentsGrouped[blogEntry.id]?.map {
                    Pair(
                        first = it.comment.commentatorHandle,
                        second = it.comment.commentatorHandleColorTag
                    )
                }?.distinctBy { it.first } ?: emptyList()
            )
        }
    }

    LazyColumn(modifier = modifier) {
        items(items = recent) {
            RecentBlogEntry(
                recentBlogEntryData = it,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 3.dp)
            )
        }
    }
}

@Immutable
private data class RecentBlogEntryData(
    val blogEntry: CodeforcesBlogEntry,
    val commentatorsHandles: List<Pair<String, CodeforcesUtils.ColorTag>>
)

@Composable
private fun RecentBlogEntry(
    recentBlogEntryData: RecentBlogEntryData,
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
            recentBlogEntryData.commentatorsHandles.forEachIndexed { index, (handle, tag) ->
                if (index > 0) append(", ")
                append(manager.makeHandleSpan(handle = handle, tag = tag))
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
        Text(
            text = title
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            //Text(text = authorHandle, modifier = Modifier.weight(1f, false))
            //Spacer(modifier = Modifier.fillMaxWidth())
            //TODO shit auto layout
            Text(
                text = commentators,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .weight(1f, false)
                    .padding(start = 5.dp)
            )
        }
    }
}