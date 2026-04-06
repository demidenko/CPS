package com.demich.cps.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.TextUnit
import com.demich.cps.platforms.Platform
import com.demich.cps.platforms.platformLogoResId

@Composable
fun PlatformIcon(
    platform: Platform,
    modifier: Modifier = Modifier,
    size: TextUnit,
    color: Color
) {
    IconSp(
        painter = platformLogoPainter(platform),
        size = size,
        modifier = modifier,
        color = color
    )
}

@Composable
fun platformLogoPainter(platform: Platform): Painter =
    painterResource(platformLogoResId(platform))
