package com.demich.cps.utils

sealed interface FetchState<out T> {
    data object Loading: FetchState<Nothing>

    data class Success<T>(val value: T): FetchState<T>

    class Failure(val exception: Throwable): FetchState<Nothing>
}

inline fun <T, R> FetchState<T>.map(transform: (T) -> R): FetchState<R> {
    return when (this) {
        is FetchState.Success -> FetchState.Success(transform(value))
        is FetchState.Failure, is FetchState.Loading -> this //!!!!! `else` not compile !!!!
    }
}

fun <T> Result<T>.toFetchResult(): FetchState<T> =
    fold(
        onSuccess = { FetchState.Success(it) },
        onFailure = { FetchState.Failure(it) }
    )

// TODO: better name and signature
data class FetchValue<out T>(
    val value: T,
    val state: FetchState<T>
) {
    constructor(value: T): this(
        value = value,
        state = FetchState.Success(value)
    )
}

operator fun <T> FetchValue<T>.plus(state: FetchState<T>): FetchValue<T> =
    when (state) {
        is FetchState.Success -> FetchValue(value = state.value, state = state)
        is FetchState.Failure, is FetchState.Loading -> copy(state = state)
    }

val FetchValue<*>.loadingStatus: LoadingStatus
    get() = when (state) {
        FetchState.Loading -> LOADING
        is FetchState.Success -> PENDING
        is FetchState.Failure -> FAILED
    }