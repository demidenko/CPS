package com.demich.kotlin_stdlib_boost

/**
 * Returns a list containing [n] random elements, preserving order.
 *
 * @throws IllegalArgumentException if [n] is negative.
 */
fun <T> List<T>.takeRandom(n: Int): List<T> {
    require(n in 0 .. size) { "Requested element count $n is out of range 0..$size" }
    return when (n) {
        0 -> emptyList()
        size -> toList()
        1 -> listOf(this.random())
        size - 1 -> {
            val index = indices.random()
            filterIndexedTo(ArrayList<T>(/*initialCapacity = */n)) { it, _ -> it != index }
        }
        else -> {
            // TODO: optimize (c++ std::sample???)
            val flip = n > size - n
            val marked = BooleanArray(size)
            var need = if (flip) size - n else n
            while (need > 0) {
                val i = indices.random()
                if (!marked[i]) {
                    marked[i] = true
                    --need
                }
            }
            filterIndexedTo(ArrayList<T>(/*initialCapacity = */n)) { it, _ -> marked[it] != flip }
        }
    }
}