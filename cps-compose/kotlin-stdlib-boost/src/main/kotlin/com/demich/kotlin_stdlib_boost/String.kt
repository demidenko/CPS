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
    from: String,
    to: String,
    block: (String) -> Unit
) {
    val i = str.indexOf(from)
    if (i == -1) return
    val j = str.indexOf(to, startIndex = i + from.length)
    if (j == -1) return
    block(str.substring(i + from.length, j))
}

inline fun ifBetweenFirstLast(
    str: String,
    from: String,
    to: String,
    include: Boolean = false,
    block: (String) -> Unit
) {
    val i = str.indexOf(from)
    if (i == -1) return
    val j = str.lastIndexOf(to)
    if (j == -1 || i + from.length > j) return
    if (include) block(str.substring(i, j + to.length))
    else block(str.substring(i + from.length, j))
}