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


fun<T> List<T>.isSortedWith(comparator: Comparator<in T>): Boolean {
    if (size < 2) return true
    for (i in 1 until size) if (comparator.compare(get(i-1),get(i)) > 0) return false
    return true
}

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

inline fun<T, R : Comparable<R>> Iterable<T>.minOfNotNull(default: R, selector: (T) -> R?): R {
    return minOfWithOrNull(comparator = nullsLast(), selector = selector) ?: default
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

//couldn't resist to note that it can be solved in O(nlogn) by suffix array + rmq
fun String.containsTokensAsSubsequence(tokens: List<String>, ignoreCase: Boolean = false): Boolean {
    var i = 0
    for (token in tokens) {
        val pos = indexOf(string = token, ignoreCase = ignoreCase, startIndex = i)
        if (pos == -1) return false
        i = pos + token.length
    }
    return true
}

inline fun <T, R> Iterable<T>.mapToSet(transform: (T) -> R): Set<R> = mapTo(mutableSetOf(), transform)

fun<K, V> Map<K, List<V>>.append(key: K, value: V): Map<K, List<V>> =
    toMutableMap().apply {
        this[key] = this[key]?.let { it + value } ?: listOf(value)
    }
