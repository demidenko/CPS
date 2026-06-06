package com.demich.cps.platforms.codeforces.lost

internal fun <K, V: Any> MutableCollection<MutableMap.MutableEntry<K, V>>.replaceValues(
    transform: (Map.Entry<K, V>) -> V?
) {
    removeAll {
        val newValue = transform(it)
        if (newValue == null) true
        else {
            it.setValue(newValue)
            false
        }
    }
}

internal fun MutableMap<Int, CodeforcesLostEntry>.put(entry: CodeforcesLostEntry) {
    put(key = entry.blogEntryId, value = entry)
}

internal fun MutableMap<Int, CodeforcesLostEntry>.upsert(entry: CodeforcesLostEntry) {
    val saved = get(entry.blogEntryId)
    if (saved == null) {
        put(entry = entry)
    } else {
        put(entry = merge(saved, entry))
    }
}

private fun merge(a: CodeforcesLostEntry, b: CodeforcesLostEntry): CodeforcesLostEntry {
    require(a.blogEntryId == b.blogEntryId) { "merge entries with different ids" }

    val authorColorTag = b.authorColorTag ?: a.authorColorTag

    // TODO: do merge
    return b
}
