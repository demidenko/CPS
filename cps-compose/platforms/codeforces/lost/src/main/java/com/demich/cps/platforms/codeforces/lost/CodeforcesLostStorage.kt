package com.demich.cps.platforms.codeforces.lost

import kotlinx.coroutines.flow.Flow

interface CodeforcesLostStorage {
    suspend fun updateData(transform: (Map<Int, CodeforcesLostEntry>) -> Map<Int, CodeforcesLostEntry>)

    suspend fun getEntries(): Map<Int, CodeforcesLostEntry>

    suspend fun flowOfLostEntries(): Flow<List<CodeforcesLostBlogEntry>>
}

suspend inline fun CodeforcesLostStorage.edit(
    crossinline block: MutableMap<Int, CodeforcesLostEntry>.() -> Unit
) {
    updateData {
        it.toMutableMap().apply(block)
    }
}

internal fun <T: CodeforcesLostEntry> MutableMap<Int, T>.put(entry: T) {
    put(key = entry.blogEntryId, value = entry)
}

internal suspend inline fun <reified T: CodeforcesLostEntry> CodeforcesLostStorage.getEntriesOf(): List<T> =
    getEntries().values.filterIsInstance<T>()