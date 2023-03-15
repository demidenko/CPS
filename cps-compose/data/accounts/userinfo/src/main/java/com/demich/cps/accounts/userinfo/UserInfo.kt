package com.demich.cps.accounts.userinfo

abstract class UserInfo {
    abstract val userId: String
    abstract val status: STATUS
    abstract val userPageUrl: String

    fun isEmpty() = userId.isBlank()
}
