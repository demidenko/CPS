package com.demich.cps.utils

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

class BackgroundDataLoader<T> (private val scope: CoroutineScope) {
    private var currentKey: Any? = null
    private var job: Job? = null
    private val mutex = Mutex()

    val flowOfFetchState: StateFlow<FetchState<T>>
        field = MutableStateFlow(value = Loading)

    private fun launchCollect(flow: Flow<T>, key: Any): Job =
        scope.launch(Dispatchers.Default) {
            flow.toFetchFlow()
                .collect {
                    mutex.withLock {
                        if (currentKey == key) flowOfFetchState.value = it
                    }
                }
        }

    private fun executeFlow(key: Any, flow: Flow<T>): StateFlow<FetchState<T>>  {
        scope.launch {
            mutex.withLock {
                if (currentKey != key) {
                    currentKey = key
                    job?.cancel()
                    job = launchCollect(flow = flow, key = key)
                }
            }
        }
        return flowOfFetchState
    }

    fun executeFlow(key: Any, block: suspend FlowCollector<T>.() -> Unit) =
        executeFlow(key = key, flow = flow(block))

    fun execute(key: Any, block: suspend () -> T) =
        executeFlow(key = key, flow = block.asFlow())
}

fun <T> ViewModel.backgroundDataLoader() = BackgroundDataLoader<T>(scope = viewModelScope)

fun ViewModel.launchData(block: suspend CoroutineScope.() -> Unit) {
    viewModelScope.launch(context = Dispatchers.Default, block = block)
}