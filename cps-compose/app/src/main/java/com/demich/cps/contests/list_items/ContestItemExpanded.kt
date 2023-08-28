package com.demich.cps.contests.list_items

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.demich.cps.ui.CPSDropdownMenuButton
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.MonospacedText
import com.demich.cps.ui.dialogs.CPSDeleteDialog
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.LocalCurrentTime
import com.demich.cps.utils.context
import com.demich.cps.utils.openUrlInBrowser

@Composable
internal fun ContestExpandedItemContent(
    contest: Contest,
    onDeleteRequest: () -> Unit
) {
    val data = contestData(
        contest = contest,
        currentTime = LocalCurrentTime.current
    )
    ContestItemHeader(
        platform = contest.platform,
        contestTitle = contest.title,
        phase = data.phase,
        modifier = Modifier.fillMaxWidth()
    )
    ContestItemFooter(
        data = data,
        modifier = Modifier.fillMaxWidth(),
        onDeleteRequest = onDeleteRequest
    )
}

@Composable
private fun ContestItemHeader(
    platform: Contest.Platform,
    contestTitle: String,
    phase: Contest.Phase,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        ContestPlatformIcon(
            platform = platform,
            modifier = Modifier
                .padding(end = 4.dp)
                .padding(all = 5.dp)
                .align(Alignment.Top),
            size = 30.sp,
            color = if (phase == Contest.Phase.FINISHED) cpsColors.contentAdditional else cpsColors.content
        )
        ContestColoredTitle(
            contestTitle = contestTitle,
            phase = phase,
            singleLine = false,
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterVertically)
        )
    }
}

@Composable
private fun ContestItemFooter(
    data: ContestData,
    modifier: Modifier = Modifier,
    onDeleteRequest: () -> Unit
) {
    ContestItemFooter(
        startTime = data.contest.startTime.contestDate(),
        endTime = data.contest.endTime.contestDate(),
        contestLink = data.contest.link,
        counter = when (data.phase) {
            Contest.Phase.BEFORE -> "starts in " + data.counter
            Contest.Phase.RUNNING -> "ends in " + data.counter
            Contest.Phase.FINISHED -> ""
        },
        modifier = modifier,
        onDeleteRequest = onDeleteRequest
    )
}

@Composable
private fun ContestItemFooter(
    startTime: String,
    endTime: String,
    contestLink: String?,
    counter: String,
    modifier: Modifier = Modifier,
    onDeleteRequest: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        ContestItemDatesAndMenuButton(
            startTime = startTime,
            endTime = endTime,
            contestLink = contestLink,
            modifier = Modifier.fillMaxWidth(),
            onDeleteRequest = onDeleteRequest
        )
        if (counter.isNotBlank()) {
            MonospacedText(
                text = counter,
                fontSize = 15.sp,
                color = cpsColors.contentAdditional
            )
        }
    }
}

@Composable
private fun ContestItemDatesAndMenuButton(
    startTime: String,
    endTime: String,
    contestLink: String?,
    modifier: Modifier = Modifier,
    onDeleteRequest: () -> Unit
) {
    Box(modifier = modifier) {
        Column(modifier = Modifier.align(Alignment.CenterStart)) {
            MonospacedText(
                text = startTime,
                fontSize = 15.sp,
                color = cpsColors.contentAdditional
            )
            MonospacedText(
                text = endTime,
                fontSize = 15.sp,
                color = cpsColors.contentAdditional
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