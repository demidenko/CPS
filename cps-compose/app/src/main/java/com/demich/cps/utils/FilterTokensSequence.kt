package com.demich.cps.utils


inline fun <T> List<T>.filterByTokensAsSubsequence(
    filter: String,
    toCheck: T.() -> Sequence<String>
): List<T> {
    val tokens = filter.trim().also {
        if (it.isEmpty()) return this
    }.split("\\s+".toRegex())

    return filter {
        it.toCheck().any { string ->
            string.containsTokensAsSubsequence(tokens = tokens, ignoreCase = true)
        }
    }
}


//couldn't resist to note that it can be solved in O(nlogn) by suffix array + rmq
fun String.containsTokensAsSubsequence(tokens: List<String>, ignoreCase: Boolean = false): Boolean {
    var i = 0
    for (token in tokens) {
        val pos = indexOf(string = token, ignoreCase = ignoreCase, startIndex = i)
        if (pos == -1) return false
        i = pos + token.length
    }
    return true
}