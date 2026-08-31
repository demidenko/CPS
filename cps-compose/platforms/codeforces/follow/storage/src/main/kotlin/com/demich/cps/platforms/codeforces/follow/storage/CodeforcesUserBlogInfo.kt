package com.demich.cps.platforms.codeforces.follow.storage

import com.demich.cps.profiles.userinfo.CodeforcesUserInfo
import com.demich.cps.profiles.userinfo.ProfileResult
import com.demich.cps.profiles.userinfo.handle

data class CodeforcesUserBlogInfo(
    val id: Long,
    val userProfile: ProfileResult<CodeforcesUserInfo>,
    val blogSize: UInt?
)

val CodeforcesUserBlogInfo.handle: String
    get() = userProfile.handle