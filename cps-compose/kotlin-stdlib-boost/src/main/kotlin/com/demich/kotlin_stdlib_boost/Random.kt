package com.demich.kotlin_stdlib_boost

fun <T> List<T>.randomSubsequence(n: Int): List<T> {
    require(n >= 0)
    return when (n) {
        0 -> emptyList()
        size -> toList()
        1 -> listOf(this.random())
        size - 1 -> {
            val index = indices.random()
            filterIndexed { it, _ -> it != index }
        }
        else -> {
            //TODO: optimize (c++ std::sample???)
            indices.shuffled().take(n).sorted().map { get(it) }
        }
    }
}