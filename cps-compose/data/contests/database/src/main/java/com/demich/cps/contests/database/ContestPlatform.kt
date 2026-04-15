package com.demich.cps.contests.database

import com.demich.cps.platforms.Platform

enum class ContestPlatform {
    unknown,
    codeforces,
    atcoder,
    codechef,
    dmoj
    ;
}

private inline fun ContestPlatform.toGeneralPlatformOr(block: () -> Nothing): Platform =
    when (this) {
        unknown -> block()
        codeforces -> codeforces
        atcoder -> atcoder
        codechef -> codechef
        dmoj -> dmoj
    }

fun ContestPlatform.toGeneralPlatformOrNull(): Platform? =
    toGeneralPlatformOr { return null }

fun ContestPlatform.toGeneralPlatform(): Platform =
    toGeneralPlatformOr { throw IllegalArgumentException() }

fun Contest.generalPlatformOrNull(): Platform? =
    platform.toGeneralPlatformOr {
        return when (host) {
            "projecteuler.net" -> project_euler
            "topcoder.com" -> topcoder
            "leetcode.com" -> leetcode
            else -> null
        }
    }