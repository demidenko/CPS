package com.demich.cps.utils

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

class BackgroundDataLoader<T> (private val scope: CoroutineScope) {
    private var currentKey: Any? = null
    private var job: Job? = null

    val flowOfFetchState: StateFlow<FetchState<T>>
        field = MutableStateFlow(value = Loading)

    fun execute(key: Any, block: suspend () -> T) =
        executeFlow(key = key) {
            emit(block())
        }

    fun executeFlow(key: Any, block: suspend FlowCollector<T>.() -> Unit): StateFlow<FetchState<T>> =
        flowOfFetchState.also {
            if (currentKey != key) {
                flowOfFetchState.value = Loading
                currentKey = key
                job?.cancel()
                job = scope.launch(Dispatchers.Default) {
                    flow(block = block)
                        .toFetchResultFlow()
                        .collect {
                            if (currentKey == key) flowOfFetchState.value = it
                        }
                }
            }
        }
}

fun <T> ViewModel.backgroundDataLoader() = BackgroundDataLoader<T>(scope = viewModelScope)

fun ViewModel.launchData(block: suspend CoroutineScope.() -> Unit) {
    viewModelScope.launch(context = Dispatchers.Default, block = block)
}