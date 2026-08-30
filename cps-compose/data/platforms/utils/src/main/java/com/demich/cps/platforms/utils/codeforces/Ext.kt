package com.demich.cps.platforms.utils.codeforces

import com.demich.cps.profiles.userinfo.CodeforcesUserInfo

val CodeforcesUserInfo.colorTag: CodeforcesColorTag
    get() = CodeforcesColorTag.fromRating(rating = rating)