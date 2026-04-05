package com.demich.cps.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.TextUnit
import com.demich.cps.R
import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.database.Contest.Platform.unknown

enum class Platform {
    codeforces,
    atcoder,
    codechef,
    topcoder,
    dmoj,
    project_euler,
    clist
}

fun Contest.Platform.toGeneralPlatformOrNull(): Platform? =
    when (this) {
        unknown -> null
        codeforces -> codeforces
        atcoder -> atcoder
        codechef -> codechef
        topcoder -> topcoder
        dmoj -> dmoj
    }

fun Contest.Platform.toGeneralPlatform(): Platform =
    toGeneralPlatformOrNull() ?: throw IllegalArgumentException()

@Composable
fun platformLogoPainter(platform: Platform): Painter {
    val iconId = when (platform) {
        codeforces -> R.drawable.ic_logo_codeforces
        atcoder -> R.drawable.ic_logo_atcoder
        topcoder -> R.drawable.ic_logo_topcoder
        codechef -> R.drawable.ic_logo_codechef
        dmoj -> R.drawable.ic_logo_dmoj
        project_euler -> R.drawable.ic_logo_projecteuler
        clist -> R.drawable.ic_logo_clist
    }
    return painterResource(iconId)
}

@Composable
fun contestPlatformLogoPainter(platform: Contest.Platform): Painter =
    when (val platform = platform.toGeneralPlatformOrNull()) {
        null -> rememberVectorPainter(CPSIcons.Contest)
        else -> platformLogoPainter(platform)
    }

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