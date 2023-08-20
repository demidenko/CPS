package com.demich.cps.contests.list_items

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.contests.ContestPlatformIcon
import com.demich.cps.contests.database.Contest
import com.demich.cps.ui.MonospacedText
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.LocalCurrentTime
import com.demich.cps.utils.append

@Composable
fun ContestItem(
    contest: Contest,
    isExpanded: () -> Boolean,
    modifier: Modifier = Modifier,
    onDeleteRequest: () -> Unit
) {
    Column(modifier = modifier) {
        if (!isExpanded()) ContestItemContent(contest = contest)
        else ContestExpandedItemContent(contest = contest, onDeleteRequest = onDeleteRequest)
    }
}

@Composable
private fun ContestItemContent(contest: Contest) {
    //TODO: recompose twice per second! (wtf?)
    val data = ContestData(
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
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ContestItemHeader(
    platform: Contest.Platform,
    contestTitle: String,
    phase: Contest.Phase,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ContestPlatformIcon(
            platform = platform,
            modifier = Modifier.padding(end = 4.dp),
            size = 18.sp,
            color = cpsColors.contentAdditional
        )
        ContestColoredTitle(
            contestTitle = contestTitle,
            phase = phase,
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
internal fun ContestColoredTitle(
    contestTitle: String,
    phase: Contest.Phase,
    singleLine: Boolean,
    modifier: Modifier = Modifier
) {
    Text(
        text = buildAnnotatedString {
            val (title, brackets) = cutTrailingBrackets(contestTitle.trim())
            append(title)
            if (brackets.isNotBlank()) append(brackets, color = cpsColors.contentAdditional)
        },
        color = when (phase) {
            Contest.Phase.BEFORE -> cpsColors.content
            Contest.Phase.RUNNING -> cpsColors.success
            Contest.Phase.FINISHED -> cpsColors.contentAdditional
        },
        fontSize = 19.sp,
        fontWeight = FontWeight.Bold,
        maxLines = if (singleLine) 1 else Int.MAX_VALUE,
        overflow = if (singleLine) TextOverflow.Ellipsis else TextOverflow.Clip,
        modifier = modifier
    )
}

@Composable
private fun ContestItemFooter(
    data: ContestData,
    modifier: Modifier = Modifier
) {
    val date: String
    val counter: String
    when (data.phase) {
        Contest.Phase.BEFORE -> {
            date = data.contest.dateRange()
            counter = "in " + data.startsIn
        }
        Contest.Phase.RUNNING -> {
            date = "ends " + data.contest.endTime.contestDate()
            counter = "left " + data.endsIn
        }
        Contest.Phase.FINISHED -> {
            date = data.contest.startTime.contestDate() + " - " + data.contest.endTime.contestDate()
            counter = ""
        }
    }

    ContestItemFooter(
        date = date,
        counter = counter,
        modifier = modifier
    )
}

@Composable
private fun ContestItemFooter(
    date: String,
    counter: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        MonospacedText(
            text = date,
            fontSize = 15.sp,
            color = cpsColors.contentAdditional,
            modifier = Modifier.align(Alignment.CenterStart)
        )
        MonospacedText(
            text = counter,
            fontSize = 15.sp,
            color = cpsColors.contentAdditional,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}