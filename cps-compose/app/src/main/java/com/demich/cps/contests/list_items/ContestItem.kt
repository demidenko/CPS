package com.demich.cps.contests.list_items

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.contests.ContestPlatformIcon
import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.dateBriefRange
import com.demich.cps.contests.dateRange
import com.demich.cps.contests.formatContestDate
import com.demich.cps.contests.isVirtual
import com.demich.cps.ui.AttentionText
import com.demich.cps.ui.CPSDefaults
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.SafetyLevel
import com.demich.cps.utils.formatTimerShort
import com.demich.cps.utils.localCurrentTime
import com.demich.cps.utils.toSystemDateTime

@Composable
fun ContestItem(
    contest: Contest,
    isExpanded: () -> Boolean,
    collisionLevel: () -> SafetyLevel,
    modifier: Modifier = Modifier,
    onDeleteRequest: () -> Unit
) {
    val expanded = isExpanded()
    Column(
        modifier = modifier,
        horizontalAlignment = if (expanded) Alignment.CenterHorizontally else Alignment.Start
    ) {
        if (!expanded) ContestItemContent(contest, collisionLevel)
        else ContestExpandedItemContent(contest, collisionLevel, onDeleteRequest = onDeleteRequest)
    }
}

@Composable
private fun ContestItemContent(
    contest: Contest,
    collisionLevel: () -> SafetyLevel
) {
    //TODO: call recomposes two times
    val phase = contest.phaseAt(localCurrentTime)
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
        collisionLevel = collisionLevel,
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
inline fun ContestItemFooter(
    modifier: Modifier = Modifier,
    crossinline left: @Composable () -> Unit,
    crossinline right: @Composable () -> Unit
) {
    ProvideContestSubtitleTextStyle {
        Box(modifier = modifier) {
            Box(modifier = Modifier.align(Alignment.CenterStart)) {
                left()
            }
            Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                right()
            }
        }
    }
}

@Composable
private fun ContestItemFooter(
    contest: Contest,
    phase: Contest.Phase,
    collisionLevel: () -> SafetyLevel,
    modifier: Modifier = Modifier
) {
    ContestItemFooter(
        modifier = modifier,
        left = {
            AttentionText(
                text = when (phase) {
                    BEFORE -> contest.dateBriefRange()
                    RUNNING -> "ends " + contest.endTime.toSystemDateTime().formatContestDate()
                    FINISHED -> contest.dateRange()
                },
                safetyLevel = if (phase == BEFORE) collisionLevel() else SAFE
            )
        },
        right = {
            ContestCounter(contest = contest)
        }
    )
}

@Composable
private fun ContestCounter(
    contest: Contest,
    modifier: Modifier = Modifier
) {
    Text(
        modifier = modifier,
        text = contest.localCurrentCounter(
            before = { "in ${it.formatTimerShort()}" },
            running = { "left ${it.formatTimerShort()}" }
        )
    )
}

@Composable
fun ProvideContestSubtitleTextStyle(
    content: @Composable () -> Unit
) {
    ProvideTextStyle(
        value = CPSDefaults.MonospaceTextStyle.copy(
            fontSize = 15.sp,
            color = cpsColors.contentAdditional
        ),
        content = content
    )
}