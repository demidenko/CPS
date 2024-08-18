package com.demich.kotlin_stdlib_boost

fun midpoint(l: Int, r: Int): Int =
    l.shr(1) + r.shr(1) + (l and r and 1)

inline fun binarySearchFirstFalse(first: Int, last: Int, predicate: (Int) -> Boolean): Int {
    var l = first
    var r = last
    while (l < r) {
        val mid = midpoint(l, r)
        if (!predicate(mid)) r = mid else l = mid + 1
    }
    return r
}

inline fun <T> List<T>.partitionPoint(predicate: (T) -> Boolean): Int = 
    binarySearchFirstFalse(first = 0, last = size) { predicate(get(it)) }
