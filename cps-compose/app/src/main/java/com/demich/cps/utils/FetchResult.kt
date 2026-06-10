package com.demich.cps.utils

sealed interface FetchResult<out T> {
    data object Loading: FetchResult<Nothing>

    class Failure(val exception: Throwable): FetchResult<Nothing>

    class Success<T>(val value: T): FetchResult<T>
}

fun <T> Result<T>?.toFetchResult(): FetchResult<T> =
    if (this == null) FetchResult.Loading
    else fold(
        onSuccess = { FetchResult.Success(it) },
        onFailure = { FetchResult.Failure(it) }
    )