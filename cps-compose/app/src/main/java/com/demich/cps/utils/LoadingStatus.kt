package com.demich.cps.utils

import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import com.demich.cps.fetchstate.FetchResult
import com.demich.cps.fetchstate.FetchState
import com.demich.cps.utils.LoadingStatus.FAILED
import com.demich.cps.utils.LoadingStatus.LOADING
import kotlinx.coroutines.flow.Flow

enum class LoadingStatus {
    PENDING, LOADING, FAILED;
}

inline fun <T> Iterable<T>.combineLoadingStatus(transform: (T) -> LoadingStatus): LoadingStatus {
    var result: LoadingStatus = PENDING
    forEach {
        val status = transform(it)
        if (status == LOADING) return LOADING
        if (status == FAILED) result = FAILED
    }
    return result
}

fun Iterable<LoadingStatus>.combine(): LoadingStatus =
    combineLoadingStatus { it }

fun Iterable<State<LoadingStatus>>.combine(): State<LoadingStatus> =
    derivedStateOf { map { it.value }.combine() }

fun Iterable<Flow<LoadingStatus>>.combine(): Flow<LoadingStatus> =
    kotlinx.coroutines.flow.combine(this) { it.asIterable().combine() }

fun <T> Result<T>?.toLoadingStatus(): LoadingStatus =
    if (this == null) LOADING
    else {
        if (isFailure) FAILED
        else PENDING
    }

fun FetchState<*>.toLoadingStatus(): LoadingStatus =
    when (this) {
        FetchState.Loading -> LOADING
        is FetchResult.Failure -> FAILED
        is FetchResult.Success -> PENDING
    }