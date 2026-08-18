package com.demich.cps.utils

import com.demich.cps.fetchstate.FetchResult
import com.demich.cps.fetchstate.FetchState

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
    get() = state.toLoadingStatus()