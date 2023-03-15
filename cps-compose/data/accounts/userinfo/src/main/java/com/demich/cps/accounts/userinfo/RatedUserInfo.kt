package com.demich.cps.accounts.userinfo

abstract class RatedUserInfo: UserInfo() {
    abstract val handle: String
    abstract val rating: Int?

    final override val userId: String
        get() = handle

    fun hasRating() = status == STATUS.OK && rating != null

    fun ratingToString() = rating?.toString() ?: "[not rated]"
}