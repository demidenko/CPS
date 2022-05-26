package com.demich.cps.news.codeforces

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.IconSp
import com.demich.cps.ui.LazyColumnWithScrollBar
import com.demich.cps.ui.itemsNotEmpty
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.LocalCurrentTime
import com.demich.cps.utils.codeforces.CodeforcesApi
import com.demich.cps.utils.codeforces.CodeforcesComment
import com.demich.cps.utils.codeforces.CodeforcesRecentAction
import com.demich.cps.utils.codeforces.CodeforcesUtils
import com.demich.cps.utils.context
import com.demich.cps.utils.openUrlInBrowser
import com.demich.cps.utils.timeAgo
import kotlin.math.roundToInt

@Composable
fun CodeforcesComments(
    commentsState: State<List<CodeforcesRecentAction>>,
    lazyListState: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier
) {
    val context = context
    LazyColumnWithScrollBar(
        state = lazyListState,
        modifier = modifier
    ) {
        itemsNotEmpty(items = commentsState.value) { recentAction ->
            Comment(
                comment = recentAction.comment,
                blogEntryTitle = recentAction.blogEntry!!.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        context.openUrlInBrowser(CodeforcesApi.urls.comment(
                            blogEntryId = recentAction.blogEntry!!.id,
                            commentId = recentAction.comment.id
                        ))
                    }
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
        commentHtml = comment.html
    )
}

@Composable
private fun Comment(
    authorHandle: AnnotatedString,
    blogEntryTitle: String,
    rating: Int,
    timeAgo: String,
    commentHtml: String,
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
        CommentBox(
            commentHtml = commentHtml,
            fontSize = 14.sp,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .fillMaxWidth()
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
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            IconSp(
                imageVector = CPSIcons.CommentSingle,
                size = 13.sp,
                color = cpsColors.contentAdditional,
                modifier = Modifier
                    .padding(start = 2.dp, end = 2.dp)
                    .alignBy { (it.measuredHeight * 0.75f).roundToInt() }
            )
            Text(
                text = authorHandle,
                fontSize = 13.sp,
                modifier = Modifier
                    .alignByBaseline()
            )
            Text(
                text = timeAgo,
                color = cpsColors.contentAdditional,
                fontSize = 11.sp,
                modifier = Modifier
                    .padding(start = 5.dp)
                    .alignByBaseline()
            )
        }
        CodeforcesUtils.VoteRatingNonZero(
            rating = rating,
            fontSize = 13.sp,
            modifier = Modifier.align(Alignment.TopEnd)
        )
    }
}

@Composable
private fun CommentBox(
    commentHtml: String,
    modifier: Modifier = Modifier,
    maxLines: Int = 10,
    fontSize: TextUnit
) {
    Box(modifier = modifier) {
        var linesOverFlow by remember { mutableStateOf(false) }
        Text(
            text = CodeforcesUtils.htmlToAnnotatedString(commentHtml),
            maxLines = maxLines,
            fontSize = fontSize,
            onTextLayout = { result ->
                linesOverFlow = result.didOverflowHeight
            }
        )
        if (linesOverFlow) {
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(cpsColors.contentAdditional.copy(alpha = 0.5f))
                .align(Alignment.BottomCenter)
            ) {

            }
        }
    }
}