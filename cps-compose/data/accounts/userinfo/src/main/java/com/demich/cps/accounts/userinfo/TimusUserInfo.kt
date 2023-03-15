package com.demich.cps.accounts.userinfo

import com.demich.cps.platforms.api.TimusApi
import kotlinx.serialization.Serializable


@Serializable
data class TimusUserInfo(
    override val status: STATUS,
    val id: String,
    val userName: String = "",
    val rating: Int = 0,
    val solvedTasks: Int = 0,
    val rankTasks: Int = 0,
    val rankRating: Int = 0
): UserInfo() {
    override val userId: String
        get() = id

    override val userPageUrl: String
        get() = TimusApi.urls.user(id.toInt())
}