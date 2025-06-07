package com.demich.kotlin_stdlib_boost

/**
 * Returns a list containing [n] random elements, preserving order.
 *
 * @throws IllegalArgumentException if [n] is negative.
 */
fun <T> List<T>.takeRandom(n: Int): List<T> {
    require(n >= 0) { "Requested element count $n is less than zero." }
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