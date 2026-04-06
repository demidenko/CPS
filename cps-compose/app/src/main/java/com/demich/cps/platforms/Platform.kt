package com.demich.cps.platforms

import com.demich.cps.R

enum class Platform {
    codeforces,
    atcoder,
    codechef,
    topcoder,
    dmoj,
    project_euler,
    clist,
    acmp,
    timus,
}

fun platformLogoResId(platform: Platform): Int =
    when (platform) {
        codeforces -> R.drawable.ic_logo_codeforces
        atcoder -> R.drawable.ic_logo_atcoder
        topcoder -> R.drawable.ic_logo_topcoder
        codechef -> R.drawable.ic_logo_codechef
        dmoj -> R.drawable.ic_logo_dmoj
        project_euler -> R.drawable.ic_logo_projecteuler
        clist -> R.drawable.ic_logo_clist
        acmp, timus -> throw IllegalArgumentException()
    }