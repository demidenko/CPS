package com.demich.kotlin_stdlib_boost


inline fun <T, R> Iterable<T>.mapToSet(transform: (T) -> R): Set<R> =
    mapTo(mutableSetOf(), transform)

fun <T> List<T>.isSortedWith(comparator: Comparator<in T>): Boolean {
    if (size < 2) return true
    for (i in 1 until size) if (comparator.compare(get(i-1),get(i)) > 0) return false
    return true
}

inline fun <T, R : Comparable<R>> Iterable<T>.minOfNotNull(selector: (T) -> R?): R? =
    minOfWithOrNull(comparator = nullsLast(), selector = selector)
