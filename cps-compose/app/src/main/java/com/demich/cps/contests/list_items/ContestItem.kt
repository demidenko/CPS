package com.demich.cps.contests.list_items

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.demich.cps.ui.AttentionText
import com.demich.cps.ui.CPSDefaults
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.DangerType
import com.demich.cps.utils.localCurrentTime
import com.demich.cps.utils.timerShort
import com.demich.cps.utils.toSystemDateTime

@Composable
fun ContestItem(
    contest: Contest,
    isExpanded: (Contest) -> Boolean,
    collisionType: (Contest) -> DangerType,
    modifier: Modifier = Modifier,
    onDeleteRequest: (Contest) -> Unit
) {
    val expanded = isExpanded(contest)
    Column(
        modifier = modifier,
        horizontalAlignment = if (expanded) Alignment.CenterHorizontally else Alignment.Start
    ) {
        if (!expanded) ContestItemContent(contest, collisionType)
        else ContestExpandedItemContent(contest, collisionType, onDeleteRequest = onDeleteRequest)
    }
}

@Composable
private fun ContestItemContent(
    contest: Contest,
    collisionType: (Contest) -> DangerType
) {
    //TODO: call recomposes two times
    val phase = contest.getPhase(localCurrentTime)
    ContestItemHeader(
        platform = contest.platform,
        contestTitle = contest.title,
        phase = phase,
        isVirtual = contest.isVirtual,
        modifier = Modifier.fillMaxWidth()
    )
    ContestItemFooter(
        contest = contest,
        phase = phase,
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
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ContestPlatformIcon(
            platform = platform,
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
    phase: Contest.Phase,
    collisionType: (Contest) -> DangerType,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        ProvideTextStyle(contestSubtitleTextStyle()) {
            AttentionText(
                text = when (phase) {
                    Contest.Phase.BEFORE -> contest.dateShortRange()
                    Contest.Phase.RUNNING -> "ends " + contest.endTime.toSystemDateTime().contestDate()
                    Contest.Phase.FINISHED -> contest.dateRange()
                },
                collisionType = if (phase == Contest.Phase.BEFORE) collisionType(contest) else DangerType.SAFE,
                modifier = Modifier.align(Alignment.CenterStart)
            )
            ContestCounter(
                contest = contest,
                phase = phase,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}

@Composable
private fun ContestCounter(
    contest: Contest,
    phase: Contest.Phase,
    modifier: Modifier = Modifier
) {
    Text(
        modifier = modifier,
        text = contest.counter(
            phase = phase,
            before = { "in ${it.timerShort()}" },
            running = { "left ${it.timerShort()}" }
        )
    )
}

@Composable
@ReadOnlyComposable
internal fun contestSubtitleTextStyle() =
    CPSDefaults.MonospaceTextStyle.copy(
        fontSize = 15.sp,
        color = cpsColors.contentAdditional
    )