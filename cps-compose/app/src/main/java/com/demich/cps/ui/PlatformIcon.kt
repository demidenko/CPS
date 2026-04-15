package com.demich.cps.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.TextUnit
import com.demich.cps.R
import com.demich.cps.platforms.Platform

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


fun platformLogoResId(platform: Platform): Int =
    when (platform) {
        codeforces -> R.drawable.ic_logo_codeforces
        atcoder -> R.drawable.ic_logo_atcoder
        topcoder -> R.drawable.ic_logo_topcoder
        codechef -> R.drawable.ic_logo_codechef
        dmoj -> R.drawable.ic_logo_dmoj
        project_euler -> R.drawable.ic_logo_projecteuler
        clist -> R.drawable.ic_logo_clist
        leetcode -> R.drawable.ic_logo_leetcode
        acmp, timus -> throw IllegalArgumentException()
    }