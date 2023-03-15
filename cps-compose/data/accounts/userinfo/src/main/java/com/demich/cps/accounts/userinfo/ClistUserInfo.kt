package com.demich.cps.accounts.userinfo

import com.demich.cps.data.api.ClistApi

data class ClistUserInfo(
    override val status: STATUS,
    val login: String,
    val accounts: Map<String, Pair<String, String>> = emptyMap()
): UserInfo() {
    override val userId: String
        get() = login

    override val userPageUrl: String
        get() = ClistApi.urls.user(login)
}