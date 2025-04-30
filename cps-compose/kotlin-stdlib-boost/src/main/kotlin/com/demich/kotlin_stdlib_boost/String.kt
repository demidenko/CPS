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