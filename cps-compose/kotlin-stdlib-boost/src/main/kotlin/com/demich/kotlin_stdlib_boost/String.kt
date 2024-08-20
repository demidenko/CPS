package com.demich.kotlin_stdlib_boost

fun trailingBracketsStart(title: String): Int {
    if (title.isEmpty() || title.last() != ')') return title.length
    var i = title.length - 2
    var ballance = 1
    while (ballance > 0 && i > 0) {
        when (title[i]) {
            '(' -> --ballance
            ')' -> ++ballance
        }
        if (ballance == 0) return i
        --i
    }
    return title.length
}

inline fun String.splitTrailingBrackets(block: (String, String) -> Unit) {
    val i = trailingBracketsStart(this)
    block(substring(0, i), substring(i))
}