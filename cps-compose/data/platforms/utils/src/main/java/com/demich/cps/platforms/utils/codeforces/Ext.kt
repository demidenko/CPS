package com.demich.cps.platforms.utils.codeforces

import com.demich.cps.platforms.api.codeforces.models.CodeforcesUser
import com.demich.cps.profiles.userinfo.CodeforcesUserInfo

fun CodeforcesUser.toUserInfo(): CodeforcesUserInfo =
    CodeforcesUserInfo(
        handle = handle,
        rating = rating,
        contribution = contribution,
        lastOnlineTime = lastOnlineTime
    )