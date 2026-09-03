package com.demich.cps.fetchstate

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.catch
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

inline fun <T, R> FetchResult<T>.map(transform: (T) -> R): FetchResult<R> {
    return when (this) {
        is FetchResult.Success -> FetchResult.Success(transform(value))
        is FetchResult.Failure -> this //!!!!! `else` not compile !!!!
    }
}

fun <T> Result<T>.asFetchResult(): FetchResult<T> =
    fold(
        onSuccess = { FetchResult.Success(it) },
        onFailure = { FetchResult.Failure(it) }
    )

fun <T> FetchResult<T>.asResult(): Result<T> =
    when (this) {
        is FetchResult.Success -> Result.success(value)
        is FetchResult.Failure -> Result.failure(exception)
    }

inline fun <T> FetchResult<T>.valueOr(onFailure: () -> T): T =
    when (this) {
        is FetchResult.Success -> value
        is FetchResult.Failure -> onFailure()
    }

private fun <T> Flow<T>.toRunCatchingFlow(): Flow<Result<T>> =
    map { Result.success(it) }
        .catch { emit(Result.failure(it)) }

fun <T> Flow<T>.toFetchResultFlow(): Flow<FetchResult<T>> =
    toRunCatchingFlow().map { it.asFetchResult() }

fun <T> Flow<T>.toFetchFlow(): Flow<FetchState<T>> =
    toFetchResultFlow()
        .onStart<FetchState<T>> {
            emit(FetchState.Loading)
        }

fun <T> fetchFlowOf(block: suspend () -> T): Flow<FetchState<T>> =
    block.asFlow().toFetchFlow()

suspend fun <T> fetchResultOf(block: suspend () -> T): FetchResult<T> =
    block.asFlow().toFetchResultFlow().single()