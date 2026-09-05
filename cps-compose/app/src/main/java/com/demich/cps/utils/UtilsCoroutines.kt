package com.demich.cps.utils

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demich.cps.ui.bottomprogressbar.ProgressBarInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration

context(collector: FlowCollector<T>)
suspend fun <T> Flow<T>.emitAll() {
    collector.emitAll(this)
}

inline fun <K, V> MutableStateFlow<Map<K, V>>.edit(block: MutableMap<K, V>.() -> Unit) =
    update { it.toMutableMap().apply(block) }

fun <K, V: Any> MutableStateFlow<Map<K, V>>.set(key: K, value: V?) =
    edit {
        if (value != null) this.set(key = key, value = value)
        else remove(key)
    }

suspend fun <A, B> awaitPair(
    blockFirst: suspend CoroutineScope.() -> A,
    blockSecond: suspend CoroutineScope.() -> B,
): Pair<A, B> {
    return coroutineScope {
        val first = async(block = blockFirst)
        val second = async(block = blockSecond)
        Pair(first.await(), second.await())
    }
}

suspend fun List<suspend () -> Unit>.joinAllWithCounter(block: suspend (Int) -> Unit) {
    block(0)
    if (isEmpty()) return
    // TODO: first crash stop other launches
    coroutineScope {
        val mutex = Mutex()
        var counter = 0
        forEach { job ->
            launch {
                try {
                    job()
                } finally {
                    mutex.withLock {
                        counter++
                        block(counter)
                    }
                }
            }
        }
    }
}

suspend fun List<suspend () -> Unit>.joinAllWithProgress(
    title: String,
    block: suspend (ProgressBarInfo) -> Unit
) {
    joinAllWithCounter {
        block(ProgressBarInfo(title = title, total = size, current = it))
    }
}

fun CoroutineScope.launchWhileActive(block: suspend CoroutineScope.() -> Duration) =
    launch {
        while (isActive) {
            val delayNext = block()
            if (delayNext == Duration.INFINITE) break
            if (delayNext > Duration.ZERO) delay(delayNext)
            ensureActive()
        }
    }

fun ViewModel.uniqueLaunch(mutex: Mutex, block: suspend CoroutineScope.() -> Unit) {
    if (!mutex.tryLock()) return
    viewModelScope.launch(context = Dispatchers.Default) {
        try {
            block()
        } finally {
            mutex.unlock()
        }
    }
}