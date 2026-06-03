package com.demich.kotlin_stdlib_boost

import java.util.Collections

fun <T> MutableList<T>.swap(i: Int, j: Int) =
    Collections.swap(this, i, j)

inline fun <T, R> Iterable<T>.mapToSet(transform: (T) -> R): Set<R> =
    mapTo(mutableSetOf(), transform)

inline fun <T, R : Comparable<R>> Iterable<T>.minOfNotNull(selector: (T) -> R?): R? =
    minOfWithOrNull(comparator = nullsLast(), selector = selector)
