package com.demich.cps.utils

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class BackgroundDataLoader<T> (private val scope: CoroutineScope) {
    private var currentId: Any? = null
    private val flow = MutableStateFlow<Result<T>?>(null)
    private var job: Job? = null

    fun flowOfResult(): StateFlow<Result<T>?> = flow

    fun execute(id: Any, block: suspend () -> T) =
        flowOfResult().also {
            if (currentId != id) {
                flow.value = null
                currentId = id
                job?.cancel()
                job = scope.launch(Dispatchers.IO) {
                    kotlin.runCatching { block() }.let {
                        if (isActive && currentId == id) flow.value = it
                    }
                }
            }
        }
}

inline fun<reified T> ViewModel.backgroundDataLoader() = BackgroundDataLoader<T>(scope = viewModelScope)