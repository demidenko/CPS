package com.demich.cps.news.codeforces

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.ui.EmptyListMessageBox
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.LocalCurrentTime
import com.demich.cps.utils.codeforces.CodeforcesApi
import com.demich.cps.utils.codeforces.CodeforcesBlogEntry
import com.demich.cps.utils.codeforces.CodeforcesUtils
import com.demich.cps.utils.context
import com.demich.cps.utils.openUrlInBrowser
import com.demich.cps.utils.timeAgo

@Composable
fun CodeforcesBlogEntries(
    blogEntriesState: State<List<CodeforcesBlogEntry>>,
    modifier: Modifier = Modifier
) {
    val context = context
    if (blogEntriesState.value.isEmpty()) {
        EmptyListMessageBox(modifier = modifier)
    } else {
        LazyColumn(
            modifier = modifier
        ) {
            items(items = blogEntriesState.value, key = { it.id }) {
                BlogEntryInfo(
                    blogEntry = it,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            context.openUrlInBrowser(CodeforcesApi.urls.blogEntry(blogEntryId = it.id))
                        }
                        .padding(horizontal = 3.dp)
                        .padding(bottom = 4.dp, top = 1.dp)
                )
                Divider()
            }
        }
    }
}

@Composable
private fun BlogEntryInfo(
    blogEntry: CodeforcesBlogEntry,
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
            commentsCount = commentsCount
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
        CodeforcesUtils.VotedText(
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
    commentsCount: Int
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 1.dp)
        ) {
            Text(
                text = authorHandle,
                fontSize = 13.sp
            )
            Text(
                text = timeAgo,
                color = cpsColors.contentAdditional,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        if (commentsCount > 0) {
            CommentsRow(
                text = AnnotatedString(commentsCount.toString()),
                fontSize = 13.sp,
                iconSize = 12.sp,
                spaceSize = 3.dp,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}