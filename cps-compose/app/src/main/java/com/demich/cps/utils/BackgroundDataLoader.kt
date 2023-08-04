package com.demich.cps.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BackgroundDataLoader<T> (private val scope: CoroutineScope) {
    private var currentId: Any? = null
    private val flow = MutableStateFlow<Result<T>?>(null)
    private var job: Job? = null

    fun flowOfResult(): StateFlow<Result<T>?> = flow

    fun execute(id: Any, block: suspend () -> T) {
        if (currentId != id) {
            currentId = id
            job?.cancel()
            flow.value = null
            job = scope.launch {
                flow.value = kotlin.runCatching { block() }
            }
        }
    }
}
