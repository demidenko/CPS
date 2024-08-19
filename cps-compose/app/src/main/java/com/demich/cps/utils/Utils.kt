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

inline fun<T> List<T>.forEach(from: Int, to: Int = size, block: (T) -> Unit) =
    (from until to).forEach { block(get(it)) }


fun Int.toSignedString(zeroAsPositive: Boolean = false): String =
    if (this > 0 || this == 0 && zeroAsPositive) "+${this}" else "$this"


inline fun<T, R: Comparable<R>> List<T>.minOfWithIndex(selector: (T) -> R): IndexedValue<R> {
    if (isEmpty()) throw NoSuchElementException()
    var indexOfMinValue = 0
    var minValue = selector(get(indexOfMinValue))
    (1 until size).forEach { index ->
        val value = selector(get(index))
        if (value < minValue) {
            minValue = value
            indexOfMinValue = index
        }
    }
    return IndexedValue(indexOfMinValue, minValue)
}

fun<T> List<T>.swapped(i: Int, j: Int): List<T> =
    toMutableList().apply {
        Collections.swap(this, i, j)
    }

inline fun<T, R> List<T>.forEachRangeEqualBy(selector: (T) -> R, block: (Int, Int) -> Unit) {
    var r = 0
    while (r < size) {
        val l = r++
        val value = selector(get(l))
        while (r < size && selector(get(r)) == value) ++r
        block(l, r)
    }
}

fun<K, V> Map<K, List<V>>.append(key: K, value: V): Map<K, List<V>> =
    toMutableMap().apply {
        this[key] = this[key]?.let { it + value } ?: listOf(value)
    }
