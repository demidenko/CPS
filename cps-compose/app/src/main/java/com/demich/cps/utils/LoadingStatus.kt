package com.demich.cps.utils

import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import com.demich.cps.utils.LoadingStatus.FAILED
import com.demich.cps.utils.LoadingStatus.LOADING
import com.demich.cps.utils.LoadingStatus.PENDING
import kotlinx.coroutines.flow.Flow

enum class LoadingStatus {
    PENDING, LOADING, FAILED;
}

fun Iterable<LoadingStatus>.combine(): LoadingStatus {
    var result = LoadingStatus.PENDING
    forEach {
        if (it == LOADING) return LOADING
        if (it == FAILED) result = FAILED
    }
    return result
}

fun Iterable<State<LoadingStatus>>.combine(): State<LoadingStatus> =
    derivedStateOf { map { it.value }.combine() }

fun Iterable<Flow<LoadingStatus>>.combine(): Flow<LoadingStatus> =
    kotlinx.coroutines.flow.combine(this) { it.asIterable().combine() }

fun<T> Result<T>?.toLoadingStatus(): LoadingStatus =
    if (this == null) LOADING
    else {
        if (isFailure) FAILED
        else PENDING
    }