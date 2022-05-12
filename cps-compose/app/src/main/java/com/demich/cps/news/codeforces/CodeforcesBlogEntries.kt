package com.demich.cps.news.codeforces

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.*
import com.demich.cps.utils.codeforces.CodeforcesApi
import com.demich.cps.utils.codeforces.CodeforcesBlogEntry
import com.demich.cps.utils.codeforces.CodeforcesUtils

@Composable
fun CodeforcesBlogEntries(
    blogEntriesState: State<List<CodeforcesBlogEntry>>,
    modifier: Modifier = Modifier
) {
    val context = context
    LazyColumn(
        modifier = modifier
    ) {
        items(items = blogEntriesState.value, key = { it.id }) {
            BlogEntryInfo(
                blogEntry = it,
                modifier = Modifier
                    .clickable { context.openUrlInBrowser(CodeforcesApi.urls.blogEntry(blogEntryId = it.id)) }
                    .padding(horizontal = 3.dp)
                    .padding(bottom = 4.dp, top = 1.dp)
                    .fillMaxWidth()
            )
            Divider()
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
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title,
                fontSize = 18.5.sp,
                modifier = Modifier.weight(1f)
            )
            CodeforcesUtils.VotedText(
                rating = rating,
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 3.dp, top = 3.dp)
            )
        }
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.align(Alignment.TopStart)) {
                Text(
                    text = buildAnnotatedString {
                        append(authorHandle)
                        append("  ")
                        append(text = timeAgo, color = cpsColors.textColorAdditional, fontSize = 11.sp)
                    },
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
            if (commentsCount > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = CPSIcons.Comments,
                        contentDescription = null,
                        tint = cpsColors.textColorAdditional,
                        modifier = Modifier
                            .padding(end = 3.dp)
                            .size(with(LocalDensity.current) { 11.sp.toDp() })
                    )
                    Text(
                        text = commentsCount.toString(),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}