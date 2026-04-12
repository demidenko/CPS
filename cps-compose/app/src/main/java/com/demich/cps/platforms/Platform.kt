package com.demich.cps.platforms

import com.demich.cps.R

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