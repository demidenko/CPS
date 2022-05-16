package com.demich.cps.news.codeforces

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.demich.cps.ui.EmptyListMessageBox
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

    if (recent.isEmpty()) {
        EmptyListMessageBox(modifier = modifier)
    } else {
        LazyColumn(modifier = modifier) {
            items(items = recent) {
                RecentBlogEntry(
                    recentBlogEntryData = it,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 3.dp, end = 3.dp, bottom = 2.dp)
                )
                Divider()
            }
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
            iconSize = 11.sp,
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
    ConstraintLayout(modifier = modifier) {
        val (author, comments) = createRefs()

        Text(
            text = authorHandle,
            fontSize = fontSize,
            maxLines = 1,
            modifier = Modifier.constrainAs(author) {
                top.linkTo(parent.top)
                start.linkTo(parent.start)
                width = Dimension.wrapContent
            }
        )

        if (commentators.isNotEmpty()) {
            //TODO: row doesn't change width on new data [only release build] (God bless ConstraintLayout-Compose)
            CommentsRow(
                text = commentators,
                fontSize = fontSize,
                iconSize = iconSize,
                spaceSize = 1.dp,
                modifier = Modifier.constrainAs(comments) {
                    top.linkTo(parent.top)
                    linkTo(
                        start = author.end,
                        end = parent.end,
                        bias = 1f,
                        startMargin = 10.dp
                    )
                    width = Dimension.preferredWrapContent
                }
            )
        }

    }
}