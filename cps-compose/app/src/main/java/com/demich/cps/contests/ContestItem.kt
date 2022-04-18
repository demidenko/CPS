package com.demich.cps.contests

import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.R
import com.demich.cps.ui.MonospacedText
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.format
import kotlin.time.Duration.Companion.days

@Composable
fun ContestItem(
    contest: Contest,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ContestPlatformIcon(
                platform = contest.platform,
                modifier = Modifier.padding(end = 4.dp),
                size = 18.sp
            )
            Text(
                text = contest.title,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(modifier = Modifier.fillMaxWidth()) {
            MonospacedText(
                text = contest.dateRange(),
                fontSize = 15.sp,
                color = cpsColors.textColorAdditional,
                modifier = Modifier.align(Alignment.CenterStart)
            )
            MonospacedText(
                text = "[todo counter]",
                fontSize = 15.sp,
                color = cpsColors.textColorAdditional,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}

@Composable
fun ContestPlatformIcon(
    platform: Contest.Platform,
    modifier: Modifier = Modifier,
    size: TextUnit
) {
    val resourceId = when (platform) {
        Contest.Platform.codeforces -> R.drawable.ic_logo_codeforces
        Contest.Platform.atcoder -> R.drawable.ic_logo_atcoder
        Contest.Platform.topcoder -> R.drawable.ic_logo_topcoder
        Contest.Platform.codechef -> R.drawable.ic_logo_codechef
        Contest.Platform.google -> R.drawable.ic_logo_google
        Contest.Platform.dmoj -> R.drawable.ic_logo_dmoj
        else -> null
    }

    val sizeDp = with(LocalDensity.current) { size.toDp() }
    val painter = if (resourceId == null)
        rememberVectorPainter(Icons.Default.EmojiEvents)
        else painterResource(id = resourceId)
    Icon(
        painter = painter,
        modifier = modifier.size(sizeDp),
        tint = cpsColors.textColorAdditional,
        contentDescription = null
    )
}

private fun Contest.dateRange(): String {
    val start = startTime.format("dd.MM E HH:mm")
    val end = if (duration < 1.days) endTime.format("HH:mm") else "..."
    return "$start-$end"
}