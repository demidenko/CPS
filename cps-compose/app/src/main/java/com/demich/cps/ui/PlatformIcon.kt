package com.demich.cps.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import com.demich.cps.R
import com.demich.cps.contests.database.Contest

enum class Platform {
    codeforces,
    atcoder,
    codechef,
    topcoder,
    dmoj,
    project_euler,
    clist
}

@Composable
fun platformIconPainter(platform: Platform): Painter {
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
fun platformIconPainter(platform: Contest.Platform): Painter =
    when (platform) {
        unknown -> rememberVectorPainter(CPSIcons.Contest)
        codeforces -> platformIconPainter(Platform.codeforces)
        atcoder -> platformIconPainter(Platform.atcoder)
        topcoder -> platformIconPainter(Platform.topcoder)
        codechef -> platformIconPainter(Platform.codechef)
        dmoj -> platformIconPainter(Platform.dmoj)
    }