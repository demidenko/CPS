package com.demich.cps.platforms.utils

import com.demich.cps.fetchstate.FetchResult
import com.demich.cps.profiles.userinfo.ProfileResult
import com.demich.cps.profiles.userinfo.UserInfo

internal fun <T> Sequence<Result<T>>.values(): Sequence<T> =
    mapNotNull { it.getOrNull() }

internal fun <T> Iterable<Result<T>>.values(): List<T> =
    mapNotNull { it.getOrNull() }

fun <U: UserInfo> FetchResult<U?>.toProfileResult(userId: String): ProfileResult<U> =
    when (this) {
        is FetchResult.Failure -> ProfileResult.Failed(userId)
        is FetchResult.Success -> value.let { value ->
            if (value != null) ProfileResult(value)
            else ProfileResult.NotFound(userId = userId)
        }
    }