package com.demich.cps.accounts.userinfo

data class UserSuggestion(
    val userId: String,
    val title: String = userId,
    val info: String = ""
)