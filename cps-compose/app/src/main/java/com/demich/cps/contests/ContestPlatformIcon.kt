package com.demich.cps.contests

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.TextUnit
import com.demich.cps.contests.database.Contest
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.IconSp
import com.demich.cps.ui.platformLogoPainter
import com.demich.cps.ui.toGeneralPlatformOrNull

@Composable
fun ContestPlatformIcon(
    platform: Contest.Platform,
    modifier: Modifier = Modifier,
    size: TextUnit,
    color: Color
) {
    IconSp(
        painter = contestPlatformLogoPainter(platform),
        size = size,
        modifier = modifier,
        color = color
    )
}

@Composable
private fun contestPlatformLogoPainter(platform: Contest.Platform): Painter =
    when (val platform = platform.toGeneralPlatformOrNull()) {
        null -> rememberVectorPainter(CPSIcons.Contest)
        else -> platformLogoPainter(platform)
    }