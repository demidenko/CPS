package com.demich.cps.utils

sealed interface FetchResult<out T> {
    data object Loading: FetchResult<Nothing>

    class Failure(val exception: Throwable): FetchResult<Nothing>

    class Success<T>(val value: T): FetchResult<T>
}

inline fun <T, R> FetchResult<T>.map(transform: (T) -> R): FetchResult<R> {
    return when (this) {
        is FetchResult.Success -> FetchResult.Success(transform(value))
        is FetchResult.Failure, is FetchResult.Loading -> this //!!!!! `else` not compile !!!!
    }
}

fun <T> Result<T>?.toFetchResult(): FetchResult<T> =
    if (this == null) FetchResult.Loading
    else fold(
        onSuccess = { FetchResult.Success(it) },
        onFailure = { FetchResult.Failure(it) }
    )