package com.demich.cps.utils

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demich.cps.fetchstate.FetchState
import com.demich.cps.fetchstate.toFetchFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class BackgroundValueHost<T>(
    private val scope: CoroutineScope,
    initValue: T
) {
    private var currentKey: Any? = null
    private var job: Job? = null
    private val mutex = Mutex()

    val flowOfValue: StateFlow<T>
        field = MutableStateFlow(value = initValue)

    fun collect(key: Any, flow: Flow<T>): StateFlow<T> {
        scope.launch {
            mutex.withLock {
                if (currentKey != key) {
                    currentKey = key
                    job?.cancel()
                    job = scope.launch(Dispatchers.Default) {
                        flow.collect {
                            mutex.withLock {
                                if (currentKey == key) flowOfValue.value = it
                            }
                        }
                    }
                }
            }
        }
        return flowOfValue
    }

    fun executeFlow(key: Any, block: suspend FlowCollector<T>.() -> Unit) =
        collect(key = key, flow = flow(block))

    fun execute(key: Any, block: suspend () -> T) =
        collect(key = key, flow = block.asFlow())
}

class BackgroundFetchStateHost<T>(scope: CoroutineScope) {
    private val host = BackgroundValueHost<FetchState<T>>(scope = scope, initValue = Loading)

    val flowOfFetchState: StateFlow<FetchState<T>>
        get() = host.flowOfValue

    private fun executeFlow(key: Any, flow: Flow<T>): StateFlow<FetchState<T>> =
        host.collect(key = key, flow = flow.toFetchFlow())

    fun executeFlow(key: Any, block: suspend FlowCollector<T>.() -> Unit) =
        executeFlow(key = key, flow = flow(block))

    fun execute(key: Any, block: suspend () -> T) =
        executeFlow(key = key, flow = block.asFlow())
}

fun <T> ViewModel.backgroundFetchStateHost() = BackgroundFetchStateHost<T>(scope = viewModelScope)
