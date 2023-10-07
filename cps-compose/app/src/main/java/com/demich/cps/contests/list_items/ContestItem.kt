package com.demich.cps.contests.list_items

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.contests.ContestPlatformIcon
import com.demich.cps.contests.contestDate
import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.dateRange
import com.demich.cps.contests.dateShortRange
import com.demich.cps.ui.CPSDefaults
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.DangerType

@Composable
fun ContestItem(
    contest: Contest,
    isExpanded: () -> Boolean,
    collisionType: () -> DangerType,
    modifier: Modifier = Modifier,
    onDeleteRequest: () -> Unit
) {
    val contestDisplay = ContestDisplay(contest, collisionType())
    AnimatedContent(
        targetState = isExpanded(),
        transitionSpec = {
            fadeIn(spring()) togetherWith fadeOut(spring())
        },
        label = ""
    ) { expanded ->
        if (!expanded) ContestItemContent(contestDisplay, modifier)
        else ContestExpandedItemContent(contestDisplay, modifier, onDeleteRequest)
    }
}

@Composable
private fun ContestItemContent(
    contestDisplay: ContestDisplay,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        val data = contestDisplay.dataByCurrentTime()
        ContestItemHeader(
            platform = data.contest.platform,
            contestTitle = data.contest.title,
            phase = data.phase,
            modifier = Modifier.fillMaxWidth()
        )
        ContestItemFooter(
            data = data,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun ContestItemHeader(
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
        ContestTitleCollapsed(
            title = contestTitle,
            phase = phase,
            modifier = Modifier.weight(1f)
        )
    }
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
            date = data.contest.dateShortRange()
            counter = "in " + data.counter
        }
        Contest.Phase.RUNNING -> {
            date = "ends " + data.contest.endTime.contestDate()
            counter = "left " + data.counter
        }
        Contest.Phase.FINISHED -> {
            date = data.contest.dateRange()
            counter = ""
        }
    }

    ContestItemFooter(
        date = date,
        counter = counter,
        collisionType = data.contestDisplay.collisionType,
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
    ProvideTextStyle(CPSDefaults.MonospaceTextStyle.copy(
        fontSize = 15.sp,
        color = cpsColors.contentAdditional
    )) {
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
