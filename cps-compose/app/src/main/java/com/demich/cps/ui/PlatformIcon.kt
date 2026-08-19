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
        codeforces -> R.drawable.logo_codeforces
        atcoder -> R.drawable.logo_atcoder
        topcoder -> R.drawable.logo_topcoder
        codechef -> R.drawable.logo_codechef
        dmoj -> R.drawable.logo_dmoj
        project_euler -> R.drawable.logo_projecteuler
        clist -> R.drawable.logo_clist
        leetcode -> R.drawable.logo_leetcode
        luogu -> R.drawable.logo_luogu
        acmp, timus -> throw IllegalArgumentException()
    }