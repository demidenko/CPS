package com.demich.cps.platforms.codeforces.lost

interface CodeforcesLostStorage {
    suspend fun update(transform: (Map<Int, CodeforcesLostEntry>) -> Map<Int, CodeforcesLostEntry>)

    suspend fun getEntries(): Map<Int, CodeforcesLostEntry>
}

internal suspend inline fun <reified T: CodeforcesLostEntry> CodeforcesLostStorage.getEntriesOf(): List<T> =
    getEntries().values.filterIsInstance<T>()

suspend inline fun CodeforcesLostStorage.editEntries(
    crossinline block: MutableMap<Int, CodeforcesLostEntry>.() -> Unit
) {
    update {
        it.toMutableMap().apply(block)
    }
}

internal fun <T: CodeforcesLostEntry> MutableMap<Int, T>.put(entry: T) {
    put(key = entry.blogEntryId, value = entry)
}

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