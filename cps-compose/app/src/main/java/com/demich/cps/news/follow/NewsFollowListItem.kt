package com.demich.cps.news.follow

import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.accounts.managers.CodeforcesUserInfo
import com.demich.cps.accounts.managers.STATUS
import com.demich.cps.news.codeforces.LocalCodeforcesAccountManager
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.IconSp
import com.demich.cps.ui.VotedRating
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.LocalCurrentTime
import com.demich.cps.utils.codeforces.CodeforcesUtils
import com.demich.cps.utils.timeAgo

@Composable
fun NewsFollowListItem(
    userInfo: CodeforcesUserInfo,
    blogEntriesCount: Int?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = LocalCodeforcesAccountManager.current.makeHandleSpan(
                    handle = userInfo.handle,
                    tag = CodeforcesUtils.getTagByRating(userInfo.rating)
                ),
                fontSize = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
            if (blogEntriesCount != null) {
                NewsFollowListItemBlogEntryCount(
                    count = blogEntriesCount,
                    iconSize = 18.sp,
                    fontSize = 15.sp
                )
            }
        }
        if (userInfo.status == STATUS.OK) {
            NewsFollowListItemInfo(
                userInfo = userInfo,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun NewsFollowListItemBlogEntryCount(
    count: Int,
    iconSize: TextUnit,
    fontSize: TextUnit,
) {
    IconSp(
        imageVector = CPSIcons.BlogEntry,
        color = cpsColors.contentAdditional,
        size = iconSize,
        modifier = Modifier.padding(end = 2.dp)
    )
    Text(
        text = count.toString(),
        fontSize = fontSize,
        color = cpsColors.content,
        modifier = Modifier
    )
}

@Composable
private fun NewsFollowListItemInfo(
    userInfo: CodeforcesUserInfo,
    modifier: Modifier = Modifier,
    fontSize: TextUnit
) {
    val currentTime = LocalCurrentTime.current
    Box(modifier = modifier) {
        Text(
            text = "online: " + timeAgo(fromTime = userInfo.lastOnlineTime, toTime = currentTime),
            color = cpsColors.contentAdditional,
            fontSize = fontSize,
            modifier = Modifier
                .align(Alignment.CenterStart)
        )
        Row(modifier = Modifier.align(Alignment.CenterEnd)) {
            Text(
                text = "cont.: ",
                color = cpsColors.contentAdditional,
                fontSize = fontSize
            )
            VotedRating(
                rating = userInfo.contribution,
                fontSize = fontSize,
                showZero = true
            )
        }
    }
}