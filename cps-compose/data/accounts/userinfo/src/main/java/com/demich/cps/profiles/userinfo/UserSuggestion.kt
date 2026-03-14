package com.demich.cps.profiles.userinfo

data class UserSuggestion(
    val userId: String,
    val title: String = userId,
    val info: String = ""
)