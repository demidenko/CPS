package com.demich.cps.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.single

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

fun FetchState<*>.toLoadingStatus(): LoadingStatus =
    when (this) {
        FetchState.Loading -> LOADING
        is FetchResult.Failure -> FAILED
        is FetchResult.Success -> PENDING
    }

fun <T> Flow<T>.toFetchResultFlow(): Flow<FetchResult<T>> =
    map<T, FetchResult<T>> { FetchResult.Success(it) }
        .catch {
            emit(FetchResult.Failure(it))
        }

fun <T> Flow<T>.toFetchFlow(): Flow<FetchState<T>> =
    map<T, FetchState<T>> { FetchResult.Success(it) }
        .onStart {
            emit(FetchState.Loading)
        }
        .catch {
            emit(FetchResult.Failure(it))
        }

fun <T> fetchFlowOf(block: suspend () -> T): Flow<FetchState<T>> =
    flow { emit(block()) }.toFetchFlow()

suspend fun <T> fetchResultOf(block: suspend () -> T): FetchResult<T> =
    flow { emit(block()) }.toFetchResultFlow().single()