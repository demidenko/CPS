package com.demich.cps.contests.list_items

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.contests.ContestPlatformIcon
import com.demich.cps.contests.contestDate
import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.dateRange
import com.demich.cps.contests.dateShortRange
import com.demich.cps.contests.isVirtual
import com.demich.cps.ui.CPSDefaults
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.DangerType
import com.demich.cps.utils.toSystemDateTime

@Composable
fun ContestItem(
    contest: Contest,
    isExpanded: () -> Boolean,
    collisionType: () -> DangerType,
    modifier: Modifier = Modifier,
    onDeleteRequest: () -> Unit
) {
    val expanded = isExpanded()
    Column(
        modifier = modifier,
        horizontalAlignment = if (expanded) Alignment.CenterHorizontally else Alignment.Start
    ) {
        //TODO: pass collisionType lambda down
        if (!expanded) ContestItemContent(contest, collisionType())
        else ContestExpandedItemContent(contest, collisionType(), onDeleteRequest = onDeleteRequest)
    }
}

@Composable
private fun ContestItemContent(
    contest: Contest,
    collisionType: DangerType
) {
    val data = dataByCurrentTime(contest)
    ContestItemHeader(
        platform = contest.platform,
        contestTitle = contest.title,
        phase = data.phase,
        isVirtual = contest.isVirtual,
        modifier = Modifier.fillMaxWidth()
    )
    ContestItemFooter(
        contest = contest,
        data = data,
        collisionType = collisionType,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun ContestItemHeader(
    platform: Contest.Platform,
    contestTitle: String,
    phase: Contest.Phase,
    isVirtual: Boolean,
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
        ContestTitleCollapsed(
            title = contestTitle,
            phase = phase,
            isVirtual = isVirtual,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ContestItemFooter(
    contest: Contest,
    data: ContestData,
    collisionType: DangerType,
    modifier: Modifier = Modifier
) {
    val date: String
    val counter: String
    when (data.phase) {
        Contest.Phase.BEFORE -> {
            date = contest.dateShortRange()
            counter = "in " + data.counter
        }
        Contest.Phase.RUNNING -> {
            date = "ends " + contest.endTime.toSystemDateTime().contestDate()
            counter = "left " + data.counter
        }
        Contest.Phase.FINISHED -> {
            date = contest.dateRange()
            counter = ""
        }
    }

    ContestItemFooter(
        date = date,
        counter = counter,
        collisionType = if (data.phase == Contest.Phase.BEFORE) collisionType else DangerType.SAFE,
        modifier = modifier
    )
}

@Composable
private fun ContestItemFooter(
    date: String,
    counter: String,
    collisionType: DangerType,
    modifier: Modifier = Modifier
) {
    ProvideTextStyle(contestSubtitleTextStyle()) {
        Box(modifier = modifier) {
            AttentionText(
                text = date,
                collisionType = collisionType,
                modifier = Modifier.align(Alignment.CenterStart)
            )
            Text(
                text = counter,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}


@Composable
@ReadOnlyComposable
internal fun contestSubtitleTextStyle() =
    CPSDefaults.MonospaceTextStyle.copy(
        fontSize = 15.5.sp,
        color = cpsColors.contentAdditional
    )