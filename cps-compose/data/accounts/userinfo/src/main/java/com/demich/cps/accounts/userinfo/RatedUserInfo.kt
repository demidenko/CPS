package com.demich.cps.accounts.userinfo

abstract class RatedUserInfo: UserInfo() {
    abstract val handle: String
    abstract val rating: Int?

    final override val userId: String
        get() = handle

}

fun RatedUserInfo.ratingToString(): String =
    rating?.toString() ?: "[not rated]"
