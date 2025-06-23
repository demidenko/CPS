package com.demich.cps.accounts.userinfo

import com.demich.cps.platforms.api.clients.ACMPUrls
import kotlinx.serialization.Serializable


@Serializable
data class ACMPUserInfo(
    val id: String,
    val userName: String = "",
    val rating: Int = 0,
    val solvedTasks: Int = 0,
    val rank: Int = 0
): UserInfo() {
    override val userId: String
        get() = id

    override val userPageUrl: String
        get() = ACMPUrls.user(id.toInt())
}