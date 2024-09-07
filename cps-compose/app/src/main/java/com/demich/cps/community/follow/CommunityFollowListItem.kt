package com.demich.cps.community.follow

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.accounts.managers.toHandleSpan
import com.demich.cps.accounts.userinfo.CodeforcesUserInfo
import com.demich.cps.accounts.userinfo.STATUS
import com.demich.cps.platforms.utils.codeforces.CodeforcesHandle
import com.demich.cps.platforms.utils.codeforces.CodeforcesUtils
import com.demich.cps.ui.AttentionIcon
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.IconSp
import com.demich.cps.ui.VotedRating
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.DangerType
import com.demich.cps.utils.localCurrentTime
import com.demich.cps.utils.timeAgo
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days

@Composable
fun CommunityFollowListItem(
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
                text = CodeforcesHandle(
                    handle = userInfo.handle,
                    colorTag = CodeforcesUtils.colorTagFrom(userInfo.rating)
                ).toHandleSpan(),
                fontSize = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
            if (blogEntriesCount != null) {
                CommunityFollowListItemBlogEntryCount(
                    count = blogEntriesCount,
                    iconSize = 18.sp,
                    fontSize = 15.sp
                )
            }
        }
        if (userInfo.status == STATUS.OK) {
            CommunityFollowListItemInfo(
                userInfo = userInfo,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun CommunityFollowListItemBlogEntryCount(
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
private fun CommunityFollowListItemInfo(
    userInfo: CodeforcesUserInfo,
    modifier: Modifier = Modifier,
    fontSize: TextUnit
) {
    Box(modifier = modifier) {
        UserOnlineInfo(
            time = userInfo.lastOnlineTime,
            fontSize = fontSize,
            modifier = Modifier.align(Alignment.CenterStart)
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

@Composable
private fun UserOnlineInfo(
    modifier: Modifier = Modifier,
    fontSize: TextUnit,
    time: Instant
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "online: " + timeAgo(fromTime = time, toTime = localCurrentTime),
            color = cpsColors.contentAdditional,
            fontSize = fontSize
        )
        if (localCurrentTime - time > 365.days) {
            AttentionIcon(
                dangerType = DangerType.WARNING,
                modifier = Modifier.padding(start = 3.dp)
            )
        }
    }
}