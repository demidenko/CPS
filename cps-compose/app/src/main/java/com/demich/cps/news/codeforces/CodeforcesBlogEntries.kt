package com.demich.cps.news.codeforces

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
import com.demich.cps.LocalCodeforcesAccountManager
import com.demich.cps.platforms.api.CodeforcesBlogEntry
import com.demich.cps.platforms.utils.codeforces.author
import com.demich.cps.ui.*
import com.demich.cps.ui.lazylist.LazyColumnOfData
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.context
import com.demich.cps.utils.localCurrentTime
import com.demich.cps.utils.timeAgo


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CodeforcesBlogEntries(
    blogEntriesController: CodeforcesBlogEntriesController,
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
        items = blogEntriesController::blogEntries,
        key = CodeforcesBlogEntry::id
    ) { blogEntry ->
        BlogEntryInfo(
            blogEntry = blogEntry,
            markNew = blogEntriesController.isNew(blogEntry.id),
            label = label,
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { blogEntriesController.openBlogEntry(blogEntry, context) },
                    onLongClick = onLongClick?.let { { it(blogEntry) } }?.withVibration()
                )
                .padding(
                    start = 5.dp,
                    end = 4.dp + (if (scrollBarEnabled) CPSDefaults.scrollBarWidth else 0.dp),
                    top = 1.dp,
                    bottom = 4.dp
                )
                .animateItemPlacement()
        )
        Divider(modifier = Modifier.animateItemPlacement())
    }
}


@Composable
private fun BlogEntryInfo(
    blogEntry: CodeforcesBlogEntry,
    markNew: Boolean,
    modifier: Modifier = Modifier,
    label: (@Composable (CodeforcesBlogEntry) -> Unit)?
) {
    val manager = LocalCodeforcesAccountManager.current

    BlogEntryInfo(
        title = blogEntry.title,
        authorHandle = manager.makeHandleSpan(blogEntry.author),
        rating = blogEntry.rating,
        commentsCount = blogEntry.commentsCount,
        timeAgo = timeAgo(
            fromTime = blogEntry.creationTime,
            toTime = localCurrentTime
        ),
        markNew = markNew,
        label = label?.let { { it(blogEntry) } },
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
            fontSize = 18.5.sp,
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
                fontSize = 13.sp,
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
                    fontSize = 13.sp,
                    iconSize = 13.5.sp,
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