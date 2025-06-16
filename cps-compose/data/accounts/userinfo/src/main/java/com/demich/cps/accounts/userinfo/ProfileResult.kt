package com.demich.cps.accounts.userinfo

import kotlinx.serialization.Serializable

@Serializable
sealed interface ProfileResult<out U: UserInfo> {
    val userId: String

    @Serializable
    data class NotFound(override val userId: String): ProfileResult<Nothing>

    @Serializable
    data class Failed(override val userId: String): ProfileResult<Nothing>

    @Serializable
    data class Success<U: UserInfo>(val userInfo: U): ProfileResult<U> {
        override val userId: String
            get() = userInfo.userId
    }
}

fun <U: UserInfo> U.toResult(): ProfileResult<U> =
    when (status) {
        STATUS.OK -> ProfileResult.Success(this)
        STATUS.NOT_FOUND -> ProfileResult.NotFound(userId)
        STATUS.FAILED -> ProfileResult.Failed(userId)
    }
