package com.demich.cps.accounts.userinfo

import com.demich.cps.platforms.api.clist.ClistUrls

data class ClistUserInfo(
    val login: String,
    val accounts: Map<String, Pair<String, String>> = emptyMap()
): UserInfo() {
    override val userId: String
        get() = login

    override val userPageUrl: String
        get() = ClistUrls.user(login)
}