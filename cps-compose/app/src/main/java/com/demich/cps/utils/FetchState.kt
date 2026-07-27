package com.demich.cps.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

sealed interface FetchState<out T> {
    data object Loading: FetchState<Nothing>
}

sealed interface FetchResult<out T>: FetchState<T> {
    data class Success<T>(val value: T): FetchResult<T>
    class Failure(val exception: Throwable): FetchResult<Nothing>
}

inline fun <T, R> FetchState<T>.map(transform: (T) -> R): FetchState<R> {
    return when (this) {
        is FetchResult.Success -> FetchResult.Success(transform(value))
        is FetchResult.Failure, is FetchState.Loading -> this //!!!!! `else` not compile !!!!
    }
}

fun <T> Result<T>.toFetchResult(): FetchResult<T> =
    fold(
        onSuccess = { FetchResult.Success(it) },
        onFailure = { FetchResult.Failure(it) }
    )

// TODO: better name and signature
data class FetchValue<out T>(
    val value: T,
    val state: FetchState<T>
) {
    constructor(value: T): this(
        value = value,
        state = FetchResult.Success(value)
    )
}

operator fun <T> FetchValue<T>.plus(state: FetchState<T>): FetchValue<T> =
    when (state) {
        is FetchResult.Success -> FetchValue(value = state.value, state = state)
        is FetchResult.Failure, is FetchState.Loading -> copy(state = state)
    }

val FetchValue<*>.loadingStatus: LoadingStatus
    get() = when (state) {
        FetchState.Loading -> LOADING
        is FetchResult.Success -> PENDING
        is FetchResult.Failure -> FAILED
    }

fun <T> Flow<T>.toFetchResultFlow(): Flow<FetchResult<T>> =
    map<T, FetchResult<T>> { FetchResult.Success(it) }
        .catch {
            emit(FetchResult.Failure(it))
        }

fun <T> Flow<T>.toFetchStateFlow(): Flow<FetchState<T>> =
    map<T, FetchState<T>> { FetchResult.Success(it) }
        .onStart {
            emit(FetchState.Loading)
        }
        .catch {
            emit(FetchResult.Failure(it))
        }