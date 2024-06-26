package com.demich.cps.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.measureTimedValue


fun<T> debugRunBlocking(block: suspend CoroutineScope.() -> T): T =
    measureTimedValue { runBlocking(block = block) }.apply {
        println("!!! ($duration) $value")
    }.value

inline fun<K, V> MutableStateFlow<Map<K, V>>.edit(block: MutableMap<K, V>.() -> Unit) =
    update { it.toMutableMap().apply(block) }

suspend fun<A, B> awaitPair(
    context: CoroutineContext = EmptyCoroutineContext,
    blockFirst: suspend CoroutineScope.() -> A,
    blockSecond: suspend CoroutineScope.() -> B,
): Pair<A, B> {
    return coroutineScope {
        val first = async(context = context, block = blockFirst)
        val second = async(context = context, block = blockSecond)
        Pair(first.await(), second.await())
    }
}

suspend fun List<suspend () -> Unit>.joinAllWithCounter(block: suspend (Int) -> Unit) {
    coroutineScope {
        val mutex = Mutex()
        var counter = 0
        block(counter)
        map { job ->
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
        }.joinAll()
    }
}