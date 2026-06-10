package com.demich.cps.utils

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BackgroundDataLoader<T> (private val scope: CoroutineScope) {
    private var currentKey: Any? = null
    private val flow = MutableStateFlow<FetchResult<T>>(FetchResult.Loading)
    private var job: Job? = null

    fun flowOfResult(): StateFlow<FetchResult<T>> = flow

    fun execute(key: Any, block: suspend () -> T) =
        flowOfResult().also {
            if (currentKey != key) {
                flow.value = FetchResult.Loading
                currentKey = key
                job?.cancel()
                job = scope.launch(Dispatchers.Default) {
                    kotlin.runCatching { block() }.let {
                        ensureActive()
                        if (currentKey == key) flow.value = it.toFetchResult()
                    }
                }
            }
        }
}

fun <T> ViewModel.backgroundDataLoader() = BackgroundDataLoader<T>(scope = viewModelScope)

fun ViewModel.launchData(block: suspend CoroutineScope.() -> Unit) {
    viewModelScope.launch(context = Dispatchers.Default, block = block)
}