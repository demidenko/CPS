package com.demich.cps.platforms.codeforces.lost

import com.demich.cps.platforms.utils.codeforces.CodeforcesColorTag
import com.demich.cps.platforms.utils.codeforces.CodeforcesWebBlogEntry
import com.demich.cps.platforms.utils.codeforces.toWebBlogEntry
import kotlinx.coroutines.flow.Flow

interface CodeforcesLostStorage {
    suspend fun update(transform: (Map<Int, CodeforcesLostEntry>) -> Map<Int, CodeforcesLostEntry>)

    suspend fun getEntries(): Map<Int, CodeforcesLostEntry>

    fun flowOfLostEntries(): Flow<List<CodeforcesLostBlogEntry>>
}

fun List<CodeforcesLostBlogEntry>.toWebBlogEntries(minColorTag: CodeforcesColorTag): List<CodeforcesWebBlogEntry> =
    mapNotNull {
        val colorTag = it.authorColorTag
        if (colorTag != null && colorTag >= minColorTag) it.blogEntry.toWebBlogEntry(colorTag)
        else null
    }

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

internal suspend inline fun <reified T: CodeforcesLostEntry> CodeforcesLostStorage.getEntriesOf(): List<T> =
    getEntries().values.filterIsInstance<T>()