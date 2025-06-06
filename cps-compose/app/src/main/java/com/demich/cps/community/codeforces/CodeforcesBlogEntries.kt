package com.demich.cps.community.codeforces

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.accounts.managers.toHandleSpan
import com.demich.cps.platforms.api.codeforces.models.CodeforcesBlogEntry
import com.demich.cps.platforms.utils.codeforces.author
import com.demich.cps.ui.*
import com.demich.cps.ui.lazylist.LazyColumnOfData
import com.demich.cps.ui.lazylist.visibleItemsInfo
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.context
import com.demich.cps.utils.localCurrentTime
import com.demich.cps.utils.timeAgo


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CodeforcesBlogEntries(
    blogEntriesState: CodeforcesBlogEntriesState,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
    scrollBarEnabled: Boolean = false,
    onLongClick: ((CodeforcesBlogEntry) -> Unit)? = null,
    label: (@Composable (CodeforcesBlogEntry) -> Unit)? = null
) {
    val context = context
    LazyColumnOfData(
        state = lazyListState,
        modifier = modifier,
        scrollBarEnabled = scrollBarEnabled,
        items = blogEntriesState::blogEntries,
        key = CodeforcesBlogEntry::id,
        contentType = { CodeforcesBlogEntryContentType }
    ) { blogEntry ->
        BlogEntryInfo(
            blogEntry = blogEntry,
            markNew = blogEntriesState.isNew(blogEntry.id),
            label = label?.let { { it(blogEntry) } },
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { blogEntriesState.openBlogEntry(blogEntry, context) },
                    onLongClick = onLongClick?.let { { it(blogEntry) } }
                )
                .padding(
                    start = 5.dp,
                    end = 4.dp + (if (scrollBarEnabled) CPSDefaults.scrollBarWidth else 0.dp),
                    top = 1.dp,
                    bottom = 4.dp
                )
                .animateItem()
        )
        Divider(modifier = Modifier.animateItem())
    }
}

private data object CodeforcesBlogEntryContentType

fun LazyListState.visibleBlogEntriesIds(requiredVisiblePart: Float): List<Int> =
    layoutInfo.visibleItemsInfo(requiredVisiblePart)
        .filter { it.contentType is CodeforcesBlogEntryContentType }
        .map { it.key as Int }

@Composable
private fun BlogEntryInfo(
    blogEntry: CodeforcesBlogEntry,
    markNew: Boolean,
    modifier: Modifier = Modifier,
    label: (@Composable () -> Unit)?
) {
    BlogEntryInfo(
        title = blogEntry.title,
        authorHandle = blogEntry.author.toHandleSpan(),
        rating = blogEntry.rating,
        commentsCount = blogEntry.commentsCount,
        timeAgo = timeAgo(
            fromTime = blogEntry.creationTime,
            toTime = localCurrentTime
        ),
        markNew = markNew,
        label = label,
        modifier = modifier
    )
}

@Composable
private fun BlogEntryInfo(
    title: String,
    authorHandle: AnnotatedString,
    rating: Int,
    commentsCount: Int,
    timeAgo: String,
    markNew: Boolean,
    modifier: Modifier = Modifier,
    label: (@Composable () -> Unit)?
) {
    Column(modifier = modifier) {
        BlogEntryInfoHeader(
            title = title,
            rating = rating
        )
        BlogEntryInfoFooter(
            authorHandle = authorHandle,
            timeAgo = timeAgo,
            commentsCount = commentsCount,
            markNew = markNew,
            label = label
        )
    }
}

@Composable
private fun BlogEntryInfoHeader(
    title: String,
    rating: Int
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 19.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        VotedRating(
            rating = rating,
            fontSize = 14.sp,
            modifier = Modifier.padding(start = 3.dp, top = 3.dp)
        )
    }
}

@Composable
private fun BlogEntryInfoFooter(
    authorHandle: AnnotatedString,
    timeAgo: String,
    commentsCount: Int,
    markNew: Boolean,
    label: (@Composable () -> Unit)?
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 1.dp)
        ) {
            Text(
                text = authorHandle,
                fontSize = 14.sp,
                modifier = Modifier.alignByBaseline()
            )
            Text(
                text = timeAgo,
                color = cpsColors.contentAdditional,
                fontSize = 11.sp,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .alignByBaseline()
            )
            if (markNew) {
                NewEntryCircle(Modifier.alignBy { it.measuredHeight })
            }
        }
        Row(modifier = Modifier.align(Alignment.TopEnd)) {
            if (label != null) label()
            if (commentsCount > 0) {
                CommentsRow(
                    text = AnnotatedString(commentsCount.toString()),
                    fontSize = 14.sp,
                    iconSize = 14.sp,
                    spaceSize = 2.dp
                )
            }
        }
    }
}

@Composable
private fun NewEntryCircle(modifier: Modifier = Modifier) {
    Box(modifier = modifier
        .padding(start = 4.dp)
        .size(6.dp)
        .clip(CircleShape)
        .background(cpsColors.newEntry)
    )
}