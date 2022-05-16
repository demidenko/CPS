package com.demich.cps.news.codeforces

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.IconSp
import com.demich.cps.ui.LazyColumnWithScrollBar
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.LocalCurrentTime
import com.demich.cps.utils.codeforces.CodeforcesComment
import com.demich.cps.utils.codeforces.CodeforcesRecentAction
import com.demich.cps.utils.codeforces.CodeforcesUtils
import com.demich.cps.utils.timeAgo

@Composable
fun CodeforcesComments(
    commentsState: State<List<CodeforcesRecentAction>>,
    modifier: Modifier = Modifier
) {
    LazyColumnWithScrollBar(modifier = modifier) {
        items(items = commentsState.value) { recentAction ->
            Comment(
                comment = recentAction.comment,
                blogEntryTitle = recentAction.blogEntry?.title ?: "",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 3.dp, end = 5.dp, bottom = 3.dp)
            )
            Divider()
        }
    }
}

@Composable
private fun Comment(
    comment: CodeforcesComment,
    blogEntryTitle: String,
    modifier: Modifier = Modifier
) {
    val manager = LocalCodeforcesAccountManager.current
    val currentTime = LocalCurrentTime.current

    Comment(
        modifier = modifier,
        authorHandle = manager.makeHandleSpan(
            handle = comment.commentatorHandle,
            tag = comment.commentatorHandleColorTag
        ),
        blogEntryTitle = blogEntryTitle,
        rating = comment.rating,
        timeAgo = timeAgo(fromTime = comment.creationTime, toTime = currentTime ),
        commentContent = comment.html
    )
}

@Composable
private fun Comment(
    authorHandle: AnnotatedString,
    blogEntryTitle: String,
    rating: Int,
    timeAgo: String,
    commentContent: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        BlogEntryTitleWithArrow(
            title = blogEntryTitle,
            fontSize = 16.sp,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        CommentInfo(
            authorHandle = authorHandle,
            rating = rating,
            timeAgo = timeAgo
        )
        Text(
            text = commentContent,
            modifier = Modifier.padding(start = 10.dp, end = 8.dp),
            maxLines = 30,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun CommentInfo(
    authorHandle: AnnotatedString,
    rating: Int,
    timeAgo: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconSp(
                imageVector = CPSIcons.CommentSingle,
                size = 11.sp,
                color = cpsColors.contentAdditional,
                modifier = Modifier.padding(start = 2.dp, end = 2.dp)
            )
            Text(
                text = authorHandle,
                fontSize = 13.sp
            )
            Text(
                text = timeAgo,
                color = cpsColors.contentAdditional,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 5.dp)
            )
        }
        CodeforcesUtils.VoteRatingNonZero(
            rating = rating,
            fontSize = 13.sp,
            modifier = Modifier.align(Alignment.TopEnd)
        )
    }
}