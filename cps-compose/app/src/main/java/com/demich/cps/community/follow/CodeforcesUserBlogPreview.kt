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
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.LocalCodeforcesProfileManager
import com.demich.cps.platforms.codeforces.follow.storage.CodeforcesUserBlogInfo
import com.demich.cps.platforms.utils.codeforces.CodeforcesRatingColorTag
import com.demich.cps.platforms.utils.codeforces.CodeforcesUnratedTag
import com.demich.cps.profiles.userinfo.CodeforcesUserInfo
import com.demich.cps.profiles.userinfo.ProfileResult
import com.demich.cps.profiles.userinfo.handle
import com.demich.cps.profiles.userinfo.userInfoOrNull
import com.demich.cps.ui.AttentionIcon
import com.demich.cps.ui.CPSFontSize
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.IconSp
import com.demich.cps.ui.VotedRating
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.contextLocalTimeZone
import com.demich.cps.utils.formatTimeAgo
import com.demich.cps.utils.localCurrentTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.yearsUntil
import kotlin.time.Instant


@Composable
fun CodeforcesUserBlogPreview(
    modifier: Modifier = Modifier,
    userBlogInfo: CodeforcesUserBlogInfo
) {
    CodeforcesUserBlogPreview(
        modifier = modifier.padding(horizontal = 8.dp, vertical = 5.dp),
        profile = userBlogInfo.userProfile,
        blogEntriesCount = userBlogInfo.blogSize
    )
}

@Composable
fun CodeforcesUserBlogPreview(
    modifier: Modifier = Modifier,
    profile: ProfileResult<CodeforcesUserInfo>,
    blogEntriesCount: Int?
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
                profile = profile,
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
        if (profile is ProfileResult.Success) {
            BottomInfo(
                userInfo = profile.userInfo,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun UserHandle(
    profile: ProfileResult<CodeforcesUserInfo>,
    modifier: Modifier = Modifier
) {
    Text(
        text = profile.toHandleSpan(),
        fontSize = CPSFontSize.itemTitle,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
@ReadOnlyComposable
private fun ProfileResult<CodeforcesUserInfo>.toHandleSpan(): AnnotatedString {
    val rating = userInfoOrNull()?.rating
    return LocalCodeforcesProfileManager.current.makeHandleSpan(
        handle = handle,
        tag = if (rating == null) CodeforcesUnratedTag else CodeforcesRatingColorTag.fromRating(rating),
        cpsColors = cpsColors
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
        text = "online: " + time.formatTimeAgo(),
        showWarning = contextLocalTimeZone { time.yearsUntil(localCurrentTime, timeZone = contextOf<TimeZone>()) > 0 }
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
                warningLevel = WARNING,
                modifier = Modifier.padding(start = 3.dp)
            )
        }
    }
}