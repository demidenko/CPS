package com.demich.cps.utils

import kotlinx.serialization.json.Json
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.time.measureTimedValue


@OptIn(ExperimentalContracts::class)
inline fun<T> debugWithDuration(
    crossinline title: (T) -> String = { "$it" },
    block: () -> T
): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return measureTimedValue(block).apply {
        println("[$duration]: ${title(value)}")
    }.value
}


val jsonCPS = Json {
    ignoreUnknownKeys = true
    allowStructuredMapKeys = true
}


fun Int.toSignedString(zeroAsPositive: Boolean = false): String =
    if (this > 0 || this == 0 && zeroAsPositive) "+${this}" else "$this"


inline fun <T, R: Comparable<R>> List<T>.minOfWithIndex(selector: (T) -> R): IndexedValue<R> {
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

inline fun <T, R> List<T>.forEachRangeEqualBy(selector: (T) -> R, block: (Int, Int) -> Unit) {
    var r = 0
    while (r < size) {
        val l = r++
        val value = selector(get(l))
        while (r < size && selector(get(r)) == value) ++r
        block(l, r)
    }
}

inline fun <K, V> MutableMap<K, List<V>>.update(key: K, block: (List<V>) -> List<V>) {
    set(key, block(getOrElse(key) { emptyList() }))
}

inline fun <K, V> MutableMap<K, List<V>>.edit(key: K, block: MutableList<V>.() -> Unit) {
    val list = get(key)?.toMutableList() ?: mutableListOf()
    set(key, list.apply(block))
}
