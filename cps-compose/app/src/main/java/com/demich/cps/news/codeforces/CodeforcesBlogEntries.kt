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
import com.demich.cps.ui.CPSDefaults
import com.demich.cps.ui.LazyColumnWithScrollBar
import com.demich.cps.ui.itemsNotEmpty
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.ui.withVibration
import com.demich.cps.utils.LocalCurrentTime
import com.demich.cps.utils.codeforces.CodeforcesBlogEntry
import com.demich.cps.utils.codeforces.CodeforcesUtils
import com.demich.cps.utils.timeAgo


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CodeforcesBlogEntries(
    blogEntriesController: CodeforcesBlogEntriesController,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
    enableScrollBar: Boolean = false,
    onLongClick: ((CodeforcesBlogEntry) -> Unit)? = null
) {
    LazyColumnWithScrollBar(
        state = lazyListState,
        modifier = modifier,
        enableScrollBar = enableScrollBar
    ) {
        itemsNotEmpty(
            items = blogEntriesController.blogEntries,
            key = { it.id }
        ) { blogEntry ->
            BlogEntryInfo(
                blogEntry = blogEntry,
                markNew = blogEntriesController.isNew(blogEntry.id),
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onLongClick = onLongClick?.let { { it(blogEntry) } }?.withVibration()
                    ) {
                        blogEntriesController.openBlogEntry(blogEntry)
                    }
                    .padding(horizontal = 3.dp)
                    .let {
                        if (enableScrollBar) it.padding(end = CPSDefaults.scrollBarWidth)
                        else it
                    }
                    .padding(bottom = 4.dp, top = 1.dp)
                    .animateItemPlacement()
            )
            Divider(modifier = Modifier.animateItemPlacement())
        }
    }
}


@Composable
private fun BlogEntryInfo(
    blogEntry: CodeforcesBlogEntry,
    markNew: Boolean,
    modifier: Modifier = Modifier
) {
    val manager = LocalCodeforcesAccountManager.current
    val currentTime = LocalCurrentTime.current

    BlogEntryInfo(
        title = blogEntry.title,
        authorHandle = manager.makeHandleSpan(
            handle = blogEntry.authorHandle,
            tag = blogEntry.authorColorTag
        ),
        rating = blogEntry.rating,
        commentsCount = blogEntry.commentsCount,
        timeAgo = timeAgo(
            fromTime = blogEntry.creationTime,
            toTime = currentTime
        ),
        markNew = markNew,
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
    modifier: Modifier = Modifier
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
            markNew = markNew
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
        CodeforcesUtils.VotedRating(
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
    markNew: Boolean
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
                Box(modifier = Modifier
                    .padding(start = 4.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(cpsColors.newEntry)
                    .alignBy { it.measuredHeight }
                )
            }
        }
        if (commentsCount > 0) {
            CommentsRow(
                text = AnnotatedString(commentsCount.toString()),
                fontSize = 13.sp,
                iconSize = 13.5.sp,
                spaceSize = 2.dp,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}