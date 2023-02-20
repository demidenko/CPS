package com.demich.cps.utils

import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import kotlinx.coroutines.flow.Flow

enum class LoadingStatus {
    PENDING, LOADING, FAILED;
}

fun Iterable<LoadingStatus>.combine(): LoadingStatus {
    var result = LoadingStatus.PENDING
    forEach {
        if (it == LoadingStatus.LOADING) return LoadingStatus.LOADING
        if (it == LoadingStatus.FAILED) result = LoadingStatus.FAILED
    }
    return result
}

fun Iterable<State<LoadingStatus>>.combine(): State<LoadingStatus> =
    derivedStateOf { map { it.value }.combine() }

fun Iterable<Flow<LoadingStatus>>.combine(): Flow<LoadingStatus> =
    kotlinx.coroutines.flow.combine(this) { it.asIterable().combine() }