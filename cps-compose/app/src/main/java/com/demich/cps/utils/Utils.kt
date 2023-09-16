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

fun<T> List<T>.swapped(i: Int, j: Int): List<T> =
    toMutableList().apply {
        Collections.swap(this, i, j)
    }

inline fun firstFalse(first: Int, last: Int, predicate: (Int) -> Boolean): Int {
    var l = first
    var r = last
    while (l < r) {
        val mid = (r - l) / 2 + l
        if (!predicate(mid)) r = mid else l = mid + 1
    }
    return r
}

//couldn't resist to note that it can be solved in O(nlogn) by suffix array + segment tree
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
