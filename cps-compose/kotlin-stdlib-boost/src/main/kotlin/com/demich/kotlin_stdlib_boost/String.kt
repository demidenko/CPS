package com.demich.kotlin_stdlib_boost

fun trailingBracketsStart(title: String): Int {
    if (title.isEmpty()) return title.length
    val closed = title.last()
    val opened = when (closed) {
        ')' -> '('
        ']' -> '['
        else -> return title.length
    }
    var i = title.length - 2
    var balance = 1
    while (balance > 0 && i > 0) {
        when (title[i]) {
            opened -> --balance
            closed -> ++balance
        }
        if (balance == 0) return i
        --i
    }
    return title.length
}

/*
    Split string like "ab cd (ef)" to "ab cd " and "(ef)"
    Otherwise calls on this and empty string
 */
inline fun String.splitTrailingBrackets(block: (String, String) -> Unit) {
    val i = trailingBracketsStart(this)
    block(substring(0, i), substring(i))
}


inline fun ifBetweenFirstFirst(
    str: String,
    a: String,
    b: String,
    block: (String) -> Unit
) {
    val i = str.indexOf(a)
    if (i == -1) return
    val j = str.indexOf(b, startIndex = i + a.length)
    if (j == -1) return
    block(str.substring(i + a.length, j))
}

inline fun ifBetweenFirstLast(
    str: String,
    a: String,
    b: String,
    block: (String) -> Unit
) {
    val i = str.indexOf(a)
    if (i == -1) return
    val j = str.lastIndexOf(b)
    if (j == -1 || i + a.length > j) return
    block(str.substring(i + a.length, j))
}