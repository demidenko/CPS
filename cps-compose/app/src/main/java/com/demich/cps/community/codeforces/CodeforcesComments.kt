package com.demich.cps.community.codeforces

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.accounts.managers.toHandleSpan
import com.demich.cps.platforms.api.codeforces.CodeforcesApi
import com.demich.cps.platforms.api.codeforces.models.CodeforcesComment
import com.demich.cps.platforms.api.codeforces.models.CodeforcesRecentAction
import com.demich.cps.platforms.utils.codeforces.commentator
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.IconSp
import com.demich.cps.ui.VotedRating
import com.demich.cps.ui.lazylist.LazyColumnOfData
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.context
import com.demich.cps.utils.htmlToAnnotatedString
import com.demich.cps.utils.localCurrentTime
import com.demich.cps.utils.openUrlInBrowser
import com.demich.cps.utils.timeAgo
import kotlin.math.roundToInt

@Composable
fun CodeforcesComments(
    comments: () -> List<CodeforcesRecentAction>,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
    showTitle: Boolean = true
) {
    val context = context
    LazyColumnOfData(
        state = lazyListState,
        scrollUpButtonEnabled = true,
        modifier = modifier,
        items = comments,
        key = { requireNotNull(it.comment).id }
    ) { recentAction ->
        val blogEntry = recentAction.blogEntry
        val comment = requireNotNull(recentAction.comment)
        Comment(
            comment = comment,
            blogEntryTitle = blogEntry?.title.takeIf { showTitle },
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (blogEntry != null) context.openUrlInBrowser(
                        CodeforcesApi.urls.comment(blogEntryId = blogEntry.id, commentId = comment.id)
                    )
                }
                .padding(start = 3.dp, end = 5.dp, bottom = 3.dp)
                .animateContentSize()
        )
        Divider()
    }
}

@Composable
private fun Comment(
    comment: CodeforcesComment,
    blogEntryTitle: String?,
    modifier: Modifier = Modifier
) {
    Comment(
        modifier = modifier,
        authorHandle = comment.commentator.toHandleSpan(),
        blogEntryTitle = blogEntryTitle,
        rating = comment.rating,
        timeAgo = timeAgo(fromTime = comment.creationTime, toTime = localCurrentTime),
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
        VotedRating(
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
    fontSize: TextUnit,
    backgroundColor: Color = cpsColors.background
) {
    Box(modifier = modifier.background(backgroundColor)) {
        var expanded by rememberSaveable { mutableStateOf(false) }
        var linesOverflow by remember { mutableStateOf(false) }
        //TODO: remove glitch on first draw
        val visible = rememberSaveable(expanded, linesOverflow) {
            !expanded && linesOverflow
        }
        Text(
            text = htmlToAnnotatedString(commentHtml),
            maxLines = if (expanded) Int.MAX_VALUE else maxLines,
            fontSize = fontSize,
            onTextLayout = { result ->
                linesOverflow = result.didOverflowHeight
            }
        )
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = EnterTransition.None,
            exit = fadeOut()
        ) {
            ExpandCommentButton(
                backgroundColor = backgroundColor,
                modifier = Modifier.fillMaxWidth(),
                onClick = { expanded = true }
            )
        }
    }
}

@Composable
private fun ExpandCommentButton(
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clickable(onClick = onClick)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(backgroundColor.copy(alpha = 0f), backgroundColor)
                )
            )
    ) {
        Icon(
            imageVector = CPSIcons.ExpandDown,
            contentDescription = null,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}