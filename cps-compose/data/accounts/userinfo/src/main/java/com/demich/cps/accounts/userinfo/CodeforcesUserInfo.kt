package com.demich.cps.accounts.userinfo

import com.demich.cps.platforms.api.codeforces.CodeforcesUrls
import com.demich.cps.platforms.api.codeforces.models.CodeforcesUser
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class CodeforcesUserInfo(
    override val handle: String,
    override val rating: Int? = null,
    val contribution: Int = 0,
    val lastOnlineTime: Instant = Instant.DISTANT_PAST
): RatedUserInfo() {
    constructor(codeforcesUser: CodeforcesUser): this(
        handle = codeforcesUser.handle,
        rating = codeforcesUser.rating,
        contribution = codeforcesUser.contribution,
        lastOnlineTime = codeforcesUser.lastOnlineTime
    )

    override val userPageUrl: String
        get() = CodeforcesUrls.user(handle)
}