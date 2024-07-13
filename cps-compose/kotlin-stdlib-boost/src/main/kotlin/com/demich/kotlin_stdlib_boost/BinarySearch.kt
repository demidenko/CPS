package com.demich.kotlin_stdlib_boost

inline fun binarySearchFirstFalse(first: Int, last: Int, predicate: (Int) -> Boolean): Int {
    var l = first
    var r = last
    while (l < r) {
        val mid = (r - l) / 2 + l
        if (!predicate(mid)) r = mid else l = mid + 1
    }
    return r
}

inline fun<T> List<T>.partitionPoint(
    fromIndex: Int = 0,
    toIndex: Int = size,
    predicate: (T) -> Boolean
): Int = binarySearchFirstFalse(first = fromIndex, last = toIndex) { predicate(get(it)) }
