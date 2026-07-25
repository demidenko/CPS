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

// TODO: better name and signature
data class FetchValue<out T>(
    val value: T,
    val lastResult: FetchResult<T>
) {
    constructor(value: T): this(
        value = value,
        lastResult = FetchResult.Success(value)
    )
}

operator fun <T> FetchValue<T>.plus(result: FetchResult<T>): FetchValue<T> =
    when (result) {
        is FetchResult.Success -> FetchValue(value = result.value, lastResult = result)
        is FetchResult.Failure, is FetchResult.Loading -> copy(lastResult = result)
    }

fun FetchValue<*>.loadingStatus(): LoadingStatus =
    when (lastResult) {
        FetchResult.Loading -> LOADING
        is FetchResult.Success -> PENDING
        is FetchResult.Failure -> FAILED
    }