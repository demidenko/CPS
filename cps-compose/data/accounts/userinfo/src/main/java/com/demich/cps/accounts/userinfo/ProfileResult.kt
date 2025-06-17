package com.demich.cps.accounts.userinfo

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

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

private val userInfoSerializersModule = SerializersModule {
    polymorphic(UserInfo::class) {
        subclass(ACMPUserInfo::class)
        subclass(AtCoderUserInfo::class)
        subclass(CodeChefUserInfo::class)
        subclass(CodeforcesUserInfo::class)
        subclass(DmojUserInfo::class)
        subclass(TimusUserInfo::class)
    }
}

val jsonProfile = Json {
    ignoreUnknownKeys = true
    serializersModule = userInfoSerializersModule
}

fun <U: UserInfo> ProfileResult<U>.userInfoOrNull(): U? =
    if (this is ProfileResult.Success) userInfo else null

// temporary converters
fun <U: UserInfo> U.asResult(): ProfileResult<U> =
    when (status) {
        STATUS.OK -> ProfileResult.Success(this)
        STATUS.NOT_FOUND -> ProfileResult.NotFound(userId)
        STATUS.FAILED -> ProfileResult.Failed(userId)
    }

fun ProfileResult<ACMPUserInfo>.toStatusUserInfo() =
    when (this) {
        is ProfileResult.Success -> userInfo
        is ProfileResult.NotFound -> ACMPUserInfo(status = STATUS.NOT_FOUND, id = userId)
        is ProfileResult.Failed -> ACMPUserInfo(status = STATUS.FAILED, id = userId)
    }

fun ProfileResult<AtCoderUserInfo>.toStatusUserInfo() =
    when (this) {
        is ProfileResult.Success -> userInfo
        is ProfileResult.NotFound -> AtCoderUserInfo(status = STATUS.NOT_FOUND, handle = userId)
        is ProfileResult.Failed -> AtCoderUserInfo(status = STATUS.FAILED, handle = userId)
    }

fun ProfileResult<CodeChefUserInfo>.toStatusUserInfo() =
    when (this) {
        is ProfileResult.Success -> userInfo
        is ProfileResult.NotFound -> CodeChefUserInfo(status = STATUS.NOT_FOUND, handle = userId)
        is ProfileResult.Failed -> CodeChefUserInfo(status = STATUS.FAILED, handle = userId)
    }

fun ProfileResult<CodeforcesUserInfo>.toStatusUserInfo() =
    when (this) {
        is ProfileResult.Success -> userInfo
        is ProfileResult.NotFound -> CodeforcesUserInfo(status = STATUS.NOT_FOUND, handle = userId)
        is ProfileResult.Failed -> CodeforcesUserInfo(status = STATUS.FAILED, handle = userId)
    }

fun ProfileResult<DmojUserInfo>.toStatusUserInfo() =
    when (this) {
        is ProfileResult.Success -> userInfo
        is ProfileResult.NotFound -> DmojUserInfo(status = STATUS.NOT_FOUND, handle = userId)
        is ProfileResult.Failed -> DmojUserInfo(status = STATUS.FAILED, handle = userId)
    }

fun ProfileResult<TimusUserInfo>.toStatusUserInfo() =
    when (this) {
        is ProfileResult.Success -> userInfo
        is ProfileResult.NotFound -> TimusUserInfo(status = STATUS.NOT_FOUND, id = userId)
        is ProfileResult.Failed -> TimusUserInfo(status = STATUS.FAILED, id = userId)
    }