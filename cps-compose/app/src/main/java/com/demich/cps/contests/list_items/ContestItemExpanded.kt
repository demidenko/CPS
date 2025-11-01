package com.demich.cps.contests.list_items

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.contests.ContestPlatformIcon
import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.database.Contest.Phase.BEFORE
import com.demich.cps.contests.dateRange
import com.demich.cps.contests.isVirtual
import com.demich.cps.platforms.api.codeforces.CodeforcesUrls
import com.demich.cps.ui.AttentionText
import com.demich.cps.ui.CPSDefaults
import com.demich.cps.ui.CPSDropdownMenuButton
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.dialogs.CPSDeleteDialog
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.DangerType
import com.demich.cps.utils.context
import com.demich.cps.utils.getCurrentTime
import com.demich.cps.utils.localCurrentTime
import com.demich.cps.utils.openUrlInBrowser
import com.demich.cps.utils.timerFull

@Composable
internal fun ContestExpandedItemContent(
    contest: Contest,
    collisionType: (Contest) -> DangerType,
    onDeleteRequest: (Contest) -> Unit
) {
    val phase = contest.getPhase(localCurrentTime)
    ContestPlatform(
        platform = contest.platform,
        platformName = contest.platformName()
    )
    ContestTitle(
        contest = contest,
        phase = phase,
    )
    ContestItemDatesAndMenuButton(
        contest = contest,
        phase = phase,
        collisionType = collisionType,
        onDeleteRequest = onDeleteRequest
    )
    ContestCounter(
        contest = contest,
        phase = phase
    )
}

private fun Contest.platformName() = host ?: platform.name

@Composable
private fun ContestPlatform(
    platform: Contest.Platform,
    platformName: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ContestPlatformIcon(
            platform = platform,
            size = 18.sp,
            color = cpsColors.contentAdditional
        )
        Text(
            text = platformName,
            style = CPSDefaults.MonospaceTextStyle.copy(
                fontSize = 13.sp,
                color = cpsColors.contentAdditional
            ),
            maxLines = 1,
            modifier = Modifier.padding(start = 5.dp)
        )
    }
}

@Composable
private fun ContestTitle(
    contest: Contest,
    phase: Contest.Phase,
) {
    ContestTitleExpanded(
        title = contest.title,
        phase = phase,
        isVirtual = contest.isVirtual,
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .fillMaxWidth()
    )
}

@Composable
private fun ContestCounter(
    contest: Contest,
    phase: Contest.Phase
) {
    ProvideTextStyle(contestSubtitleTextStyle()) {
        Text(
            text = contest.counter(
                phase = phase,
                before = { "starts in ${it.timerFull()}" },
                running = { "ends in ${it.timerFull()}" },
                finished = { "finished" }
            )
        )
    }
}

@Composable
private fun ContestItemDatesAndMenuButton(
    contest: Contest,
    phase: Contest.Phase,
    collisionType: (Contest) -> DangerType,
    onDeleteRequest: (Contest) -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        ProvideTextStyle(contestSubtitleTextStyle()) {
            AttentionText(
                text = contest.dateRange(),
                collisionType = if (phase == BEFORE) collisionType(contest) else DangerType.SAFE,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        ContestItemMenuButton(
            contest = contest,
            modifier = Modifier.align(Alignment.CenterEnd),
            onDeleteRequest = onDeleteRequest
        )
    }
}

@Composable
private fun ContestItemMenuButton(
    contest: Contest,
    modifier: Modifier = Modifier,
    onDeleteRequest: (Contest) -> Unit
) {
    val context = context
    var showDeleteDialog by remember { mutableStateOf(false) }
    CPSDropdownMenuButton(
        icon = CPSIcons.More,
        color = cpsColors.contentAdditional,
        iconSize = 22.dp,
        modifier = modifier
    ) {
        if (contest.link != null) {
            CPSDropdownMenuItem(title = "Open in browser", icon = CPSIcons.OpenInBrowser) {
                contest.properLink()?.let { context.openUrlInBrowser(it) }
            }
        }
        CPSDropdownMenuItem(title = "Delete", icon = CPSIcons.Delete) {
            showDeleteDialog = true
        }
    }
    if (showDeleteDialog) {
        CPSDeleteDialog(
            title = "Delete contest from list?",
            onDismissRequest = { showDeleteDialog = false },
            onConfirmRequest = { onDeleteRequest(contest) }
        )
    }
}

private fun Contest.properLink(): String? {
    if (platform == Contest.Platform.codeforces) {
        val contestId = id.toIntOrNull() ?: return link
        return when (getPhase(currentTime = getCurrentTime())) {
            BEFORE -> CodeforcesUrls.contestPending(contestId = contestId)
            else -> CodeforcesUrls.contest(contestId = contestId)
        }
    }
    return link
}
