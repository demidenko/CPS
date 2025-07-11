package com.demich.cps.community.follow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.accounts.managers.toHandleSpan
import com.demich.cps.accounts.userinfo.CodeforcesUserInfo
import com.demich.cps.platforms.utils.codeforces.CodeforcesHandle
import com.demich.cps.platforms.utils.codeforces.CodeforcesUtils
import com.demich.cps.ui.AttentionIcon
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.IconSp
import com.demich.cps.ui.VotedRating
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.DangerType
import com.demich.cps.utils.localCurrentTime
import com.demich.cps.utils.toTimeAgoString
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

@Composable
fun CommunityFollowListItem(
    handle: String,
    userInfo: CodeforcesUserInfo?,
    blogEntriesCount: Int?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UserHandle(
                handle = handle,
                userInfo = userInfo,
                modifier = Modifier.weight(1f)
            )
            if (blogEntriesCount != null) {
                BlogEntryCount(
                    count = blogEntriesCount,
                    iconSize = 18.sp,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
        if (userInfo != null) {
            BottomInfo(
                userInfo = userInfo,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun UserHandle(
    handle: String,
    userInfo: CodeforcesUserInfo?,
    modifier: Modifier = Modifier
) {
    Text(
        text = CodeforcesHandle(
            handle = userInfo?.handle ?: handle,
            colorTag = CodeforcesUtils.colorTagFrom(userInfo?.rating)
        ).toHandleSpan(),
        fontSize = 18.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
private fun BlogEntryCount(
    count: Int,
    iconSize: TextUnit,
    fontSize: TextUnit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        IconSp(
            imageVector = CPSIcons.BlogEntry,
            color = cpsColors.contentAdditional,
            size = iconSize
        )
        Text(
            text = count.toString(),
            fontSize = fontSize,
            color = cpsColors.content
        )
    }
}

@Composable
private fun BottomInfo(
    userInfo: CodeforcesUserInfo,
    modifier: Modifier = Modifier,
    fontSize: TextUnit
) {
    ProvideTextStyle(TextStyle(fontSize = fontSize, color = cpsColors.contentAdditional)) {
        Box(modifier = modifier) {
            UserOnlineInfo(
                time = userInfo.lastOnlineTime,
                modifier = Modifier.align(Alignment.CenterStart)
            )
            Row(modifier = Modifier.align(Alignment.CenterEnd)) {
                Text(text = "cont.: ")
                VotedRating(
                    rating = userInfo.contribution,
                    showZero = true
                )
            }
        }
    }
}

@Composable
private fun UserOnlineInfo(
    modifier: Modifier = Modifier,
    time: Instant
) {
    UserOnlineInfo(
        modifier = modifier,
        text = "online: " + time.toTimeAgoString(),
        showWarning = localCurrentTime - time > 365.days
    )
}

@Composable
private fun UserOnlineInfo(
    modifier: Modifier = Modifier,
    text: String,
    showWarning: Boolean
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(text = text)
        if (showWarning) {
            AttentionIcon(
                dangerType = DangerType.WARNING,
                modifier = Modifier.padding(start = 3.dp)
            )
        }
    }
}