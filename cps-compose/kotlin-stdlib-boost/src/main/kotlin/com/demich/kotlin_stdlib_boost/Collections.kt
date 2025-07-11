package com.demich.kotlin_stdlib_boost

import java.util.Collections
import java.util.EnumSet

fun <T> MutableList<T>.swap(i: Int, j: Int) =
    Collections.swap(this, i, j)

inline fun <T, R> Iterable<T>.mapToSet(transform: (T) -> R): Set<R> =
    mapTo(mutableSetOf(), transform)

inline fun <reified T: Enum<T>> Collection<T>.toEnumSet(): EnumSet<T> =
    if (isEmpty()) EnumSet.noneOf(T::class.java)
    else EnumSet.copyOf(this)

fun <T> Iterable<T>.isSortedWith(comparator: Comparator<in T>): Boolean {
    if (this is Collection<T> && size < 2) return true
    val iterator = iterator()
    if (!iterator.hasNext()) return true
    var current = iterator.next()
    while (iterator.hasNext()) {
        val next = iterator.next()
        if (comparator.compare(current, next) > 0) return false
        current = next
    }
    return true
}

inline fun <T, R : Comparable<R>> Iterable<T>.minOfNotNull(selector: (T) -> R?): R? =
    minOfWithOrNull(comparator = nullsLast(), selector = selector)
