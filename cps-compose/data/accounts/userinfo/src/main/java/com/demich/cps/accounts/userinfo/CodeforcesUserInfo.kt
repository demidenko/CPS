package com.demich.cps.accounts.userinfo

import com.demich.cps.platforms.api.InstantAsSecondsSerializer
import com.demich.cps.platforms.api.codeforces.CodeforcesUrls
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class CodeforcesUserInfo(
    override val handle: String,
    override val rating: Int? = null,
    val contribution: Int = 0,
    @Serializable(with = InstantAsSecondsSerializer::class)
    val lastOnlineTime: Instant
): RatedUserInfo() {
    override val userPageUrl: String
        get() = CodeforcesUrls.user(handle)
}