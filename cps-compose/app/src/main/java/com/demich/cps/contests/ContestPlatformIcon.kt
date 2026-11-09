package com.demich.cps.contests

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.TextUnit
import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.database.generalPlatformOrNull
import com.demich.cps.platforms.Platform
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.IconSp
import com.demich.cps.ui.platformLogoPainter

@Composable
fun ContestPlatformIcon(
    platform: Platform?,
    modifier: Modifier = Modifier,
    size: TextUnit,
    color: Color
) {
    IconSp(
        painter = platform?.let { platformLogoPainter(it) } ?: rememberVectorPainter(CPSIcons.Contest),
        size = size,
        modifier = modifier,
        color = color
    )
}

@Composable
fun ContestPlatformIcon(
    contest: Contest,
    modifier: Modifier = Modifier,
    size: TextUnit,
    color: Color
) {
    ContestPlatformIcon(
        platform = contest.generalPlatformOrNull(),
        size = size,
        modifier = modifier,
        color = color
    )
}