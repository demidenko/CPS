package com.demich.cps.news.follow

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.demich.cps.accounts.managers.CodeforcesUserInfo
import com.demich.cps.accounts.managers.STATUS
import com.demich.cps.news.codeforces.LocalCodeforcesAccountManager
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.LocalCurrentTime
import com.demich.cps.utils.append
import com.demich.cps.utils.codeforces.CodeforcesUtils
import com.demich.cps.utils.signedToString
import com.demich.cps.utils.timeAgo

@Composable
fun NewsFollowListItem(
    userInfo: CodeforcesUserInfo,
    blogEntriesCount: Int?,
    modifier: Modifier = Modifier
) {
    val manager = LocalCodeforcesAccountManager.current

    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = manager.makeHandleSpan(
                    handle = userInfo.handle,
                    tag = CodeforcesUtils.getTagByRating(userInfo.rating)
                ),
                fontSize = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .alignByBaseline()
            )
            if (blogEntriesCount != null) {
                Text(
                    text = blogEntriesCount.toString(),
                    fontSize = 15.sp,
                    color = cpsColors.content,
                    modifier = Modifier
                        .alignByBaseline()
                )
            }
        }
        if (userInfo.status == STATUS.OK) {
            NewsFollowListItemInfo(
                userInfo = userInfo,
                modifier = Modifier.fillMaxWidth()
            )
        }

    }
}

@Composable
private fun NewsFollowListItemInfo(
    userInfo: CodeforcesUserInfo,
    modifier: Modifier
) {
    val currentTime = LocalCurrentTime.current
    Box(modifier = modifier) {
        Text(
            text = "online: " + timeAgo(fromTime = userInfo.lastOnlineTime, toTime = currentTime),
            color = cpsColors.contentAdditional,
            fontSize = 13.sp,
            modifier = Modifier
                .align(Alignment.CenterStart)
        )
        Text(
            text = buildAnnotatedString {
                append(
                    text = "cont.: ",
                    color = cpsColors.contentAdditional
                )
                append(
                    text = signedToString(userInfo.contribution),
                    color = if(userInfo.contribution > 0) cpsColors.votedRatingPositive else cpsColors.votedRatingNegative,
                    fontWeight = FontWeight.Bold
                )
            },
            fontSize = 13.sp,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}