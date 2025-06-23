package com.demich.cps.accounts.userinfo

import com.demich.cps.platforms.api.codeforces.CodeforcesUrls
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class CodeforcesUserInfo(
    override val handle: String,
    override val rating: Int? = null,
    val contribution: Int = 0,
    val lastOnlineTime: Instant = Instant.DISTANT_PAST
): RatedUserInfo() {
    override val userPageUrl: String
        get() = CodeforcesUrls.user(handle)
}