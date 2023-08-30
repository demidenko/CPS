package com.demich.cps.utils

import kotlinx.serialization.json.Json
import java.util.Collections
import kotlin.time.measureTimedValue


inline fun<T> debugDuration(block: () -> T): T =
    measureTimedValue(block).apply {
        println("duration = $duration")
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

fun<K, V> Map<K, List<V>>.append(key: K, value: V): Map<K, List<V>> =
    toMutableMap().apply {
        this[key] = this[key]?.let { it + value } ?: listOf(value)
    }
