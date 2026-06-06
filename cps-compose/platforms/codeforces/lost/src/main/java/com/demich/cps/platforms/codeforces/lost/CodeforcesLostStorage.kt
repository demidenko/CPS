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