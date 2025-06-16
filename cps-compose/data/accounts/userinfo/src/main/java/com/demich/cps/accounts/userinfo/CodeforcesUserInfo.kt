package com.demich.cps.accounts.userinfo

import com.demich.cps.platforms.api.codeforces.CodeforcesApi
import com.demich.cps.platforms.api.codeforces.models.CodeforcesUser
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class CodeforcesUserInfo(
    override val status: STATUS,
    override val handle: String,
    override val rating: Int? = null,
    val contribution: Int = 0,
    val lastOnlineTime: Instant = Instant.DISTANT_PAST
): RatedUserInfo() {
    constructor(codeforcesUser: CodeforcesUser): this(
        status = STATUS.OK,
        handle = codeforcesUser.handle,
        rating = codeforcesUser.rating,
        contribution = codeforcesUser.contribution,
        lastOnlineTime = codeforcesUser.lastOnlineTime
    )

    override val userPageUrl: String
        get() = CodeforcesApi.urls.user(handle)
}

fun ProfileResult<CodeforcesUserInfo>.toStatusUserInfo() =
    when (this) {
        is ProfileResult.Success -> userInfo
        is ProfileResult.NotFound -> CodeforcesUserInfo(status = STATUS.NOT_FOUND, handle = userId)
        is ProfileResult.Failed -> CodeforcesUserInfo(status = STATUS.FAILED, handle = userId)
    }