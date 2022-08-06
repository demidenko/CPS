package com.demich.cps.news.codeforces

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
    modifier: Modifier = Modifier,
    showTitle: Boolean = true
) {
    val context = context
    LazyColumnWithScrollBar(
        state = lazyListState,
        modifier = modifier
    ) {
        itemsNotEmpty(
            items = commentsState.value,
            key = { it.comment.id }
        ) { recentAction ->
            val blogEntry = recentAction.blogEntry!!
            val comment = recentAction.comment
            Comment(
                comment = comment,
                blogEntryTitle = blogEntry.title.takeIf { showTitle },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        context.openUrlInBrowser(CodeforcesApi.urls.comment(
                            blogEntryId = blogEntry.id,
                            commentId = comment.id
                        ))
                    }
                    .padding(start = 3.dp, end = 5.dp, bottom = 3.dp)
                    .animateContentSize()
            )
            Divider()
        }
    }
}

@Composable
private fun Comment(
    comment: CodeforcesComment,
    blogEntryTitle: String?,
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
    blogEntryTitle: String?,
    rating: Int,
    timeAgo: String,
    commentHtml: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        if (blogEntryTitle != null) {
            BlogEntryTitleWithArrow(
                title = blogEntryTitle,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
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
        CodeforcesUtils.VotedRating(
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
        var expanded by rememberSaveable { mutableStateOf(false) }
        var linesOverflow by remember { mutableStateOf(false) }
        Text(
            text = CodeforcesUtils.htmlToAnnotatedString(commentHtml),
            maxLines = if (expanded) Int.MAX_VALUE else maxLines,
            fontSize = fontSize,
            onTextLayout = { result ->
                linesOverflow = result.didOverflowHeight
            }
        )
        if (!expanded && linesOverflow) {
            ExpandCommentButton(
                onClick = { expanded = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(48.dp)
            )
        }
    }
}

@Composable
private fun ExpandCommentButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(modifier
        .background(
            brush = Brush.verticalGradient(listOf(
                cpsColors.background.copy(alpha = 0f),
                cpsColors.background.copy(alpha = 1f)
            ))
        ).clickable(onClick = onClick),
    ) {
        Icon(
            imageVector = CPSIcons.ExpandDown,
            contentDescription = null,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}