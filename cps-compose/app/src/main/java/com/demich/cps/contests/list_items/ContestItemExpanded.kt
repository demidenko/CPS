package com.demich.cps.contests.list_items

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.demich.cps.contests.dateRange
import com.demich.cps.ui.CPSDefaults
import com.demich.cps.ui.CPSDropdownMenuButton
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.dialogs.CPSDeleteDialog
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.DangerType
import com.demich.cps.utils.context
import com.demich.cps.utils.openUrlInBrowser

@Composable
internal fun ContestExpandedItemContent(
    contestDisplay: ContestDisplay,
    onDeleteRequest: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val data = contestDisplay.dataByCurrentTime()
        ContestPlatform(
            platform = data.contest.platform,
            platformName = data.contest.platformName()
        )
        ContestTitle(
            contestTitle = data.contest.title,
            phase = data.phase
        )
        ContestItemDatesAndMenuButton(
            contestDisplay = contestDisplay,
            onDeleteRequest = onDeleteRequest
        )
        ContestCounter(
            counter = when (data.phase) {
                Contest.Phase.BEFORE -> "starts in " + data.counter
                Contest.Phase.RUNNING -> "ends in " + data.counter
                Contest.Phase.FINISHED -> ""
            }
        )
    }
}

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
    contestTitle: String,
    phase: Contest.Phase
) {
    ContestTitleExpanded(
        title = contestTitle,
        phase = phase,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ContestItemDatesAndMenuButton(
    contestDisplay: ContestDisplay,
    onDeleteRequest: () -> Unit
) {
    ContestItemDatesAndMenuButton(
        dateRange = contestDisplay.contest.dateRange(),
        contestLink = contestDisplay.contest.link,
        collisionType = contestDisplay.collisionType,
        modifier = Modifier.fillMaxWidth(),
        onDeleteRequest = onDeleteRequest
    )
}

@Composable
private fun ContestCounter(
    counter: String
) {
    if (counter.isNotBlank()) {
        Text(
            text = counter,
            style = CPSDefaults.MonospaceTextStyle.copy(
                fontSize = 15.sp,
                color = cpsColors.contentAdditional
            )
        )
    }
}

@Composable
private fun ContestItemDatesAndMenuButton(
    dateRange: String,
    collisionType: DangerType,
    contestLink: String?,
    modifier: Modifier = Modifier,
    onDeleteRequest: () -> Unit
) {
    Box(modifier = modifier) {
        ProvideTextStyle(CPSDefaults.MonospaceTextStyle.copy(
            fontSize = 15.sp,
            color = cpsColors.contentAdditional
        )) {
            AttentionText(
                text = dateRange,
                collisionType = collisionType,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        ContestItemMenuButton(
            contestLink = contestLink,
            modifier = Modifier.align(Alignment.CenterEnd),
            onDeleteRequest = onDeleteRequest
        )
    }
}

@Composable
private fun ContestItemMenuButton(
    contestLink: String?,
    modifier: Modifier = Modifier,
    onDeleteRequest: () -> Unit
) {
    val context = context
    var showDeleteDialog by remember { mutableStateOf(false) }
    CPSDropdownMenuButton(
        icon = CPSIcons.More,
        color = cpsColors.contentAdditional,
        iconSize = 22.dp,
        modifier = modifier
    ) {
        if (contestLink != null) {
            CPSDropdownMenuItem(title = "Open in browser", icon = CPSIcons.OpenInBrowser) {
                context.openUrlInBrowser(contestLink)
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
            onConfirmRequest = onDeleteRequest
        )
    }
}