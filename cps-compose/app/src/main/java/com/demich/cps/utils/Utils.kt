package com.demich.cps.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.util.Collections
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.measureTimedValue


inline fun<T> debugDuration(block: () -> T): T =
    measureTimedValue(block).apply {
        println("duration = $duration")
    }.value

fun<T> debugRunBlocking(block: suspend CoroutineScope.() -> T): T =
    measureTimedValue { runBlocking(block = block) }.apply {
        println("!!! ($duration) $value")
    }.value


val jsonCPS = Json {
    ignoreUnknownKeys = true
    allowStructuredMapKeys = true
}



fun Int.toSignedString(zeroAsPositive: Boolean = false): String =
    if (this > 0 || this == 0 && zeroAsPositive) "+${this}" else "$this"


fun <T> List<T>.isSortedWith(comparator: Comparator<in T>): Boolean {
    if (size < 2) return true
    for (i in 1 until size) if (comparator.compare(get(i-1),get(i)) > 0) return false
    return true
}

fun<T> List<T>.swapped(i: Int, j: Int): List<T> =
    toMutableList().apply {
        Collections.swap(this, i, j)
    }

inline fun firstFalse(first: Int, last: Int, pred: (Int) -> Boolean): Int {
    var l = first
    var r = last
    while (l < r) {
        val mid = (r - l) / 2 + l
        if (!pred(mid)) r = mid else l = mid + 1
    }
    return r
}

inline fun <T, R> Iterable<T>.mapToSet(transform: (T) -> R): Set<R> = mapTo(mutableSetOf(), transform)

inline fun<K, V> MutableStateFlow<Map<K, V>>.edit(block: MutableMap<K, V>.() -> Unit) =
    update { it.toMutableMap().apply(block) }

fun<K, V> Map<K, List<V>>.append(key: K, value: V): Map<K, List<V>> =
    toMutableMap().apply {
        this[key] = this[key]?.let { it + value } ?: listOf(value)
    }

fun<K, V> Map<K, Flow<V>>.combine(): Flow<Map<K, V>> =
    combine(entries.map { (key, value) -> value.map { key to it } }) { it.toMap() }

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