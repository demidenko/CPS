package com.demich.cps.contests

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.TextUnit
import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.database.ContestPlatform
import com.demich.cps.contests.database.generalPlatformOrNull
import com.demich.cps.contests.database.toGeneralPlatformOrNull
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
        painter = when (platform) {
            null -> rememberVectorPainter(CPSIcons.Contest)
            else -> platformLogoPainter(platform)
        },
        size = size,
        modifier = modifier,
        color = color
    )
}

@Composable
fun ContestPlatformIcon(
    platform: ContestPlatform,
    modifier: Modifier = Modifier,
    size: TextUnit,
    color: Color
) {
    ContestPlatformIcon(
        platform = platform.toGeneralPlatformOrNull(),
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