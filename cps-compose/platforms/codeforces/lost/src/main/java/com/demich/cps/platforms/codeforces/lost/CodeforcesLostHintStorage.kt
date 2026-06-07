package com.demich.cps.platforms.codeforces.lost

import kotlinx.serialization.Serializable
import kotlin.time.Instant


@Serializable
data class CodeforcesLostHint(
    val blogEntryId: Int,
    val creationTime: Instant
)

interface CodeforcesLostHintStorage {
    suspend fun getValue(): CodeforcesLostHint?

    suspend fun update(transform: (CodeforcesLostHint?) -> CodeforcesLostHint?)
}

internal class CheckedHintStorage(
    private val storage: CodeforcesLostHintStorage,
    val isFresh: (Instant) -> Boolean
) {
    suspend fun getHint(): CodeforcesLostHint? {
        val hint = storage.getValue()
        // ensure hint in case isFresh logic changes
        if (hint != null && isFresh(hint.creationTime)) {
            storage.update { null }
            return null
        } else {
            return hint
        }
    }

    suspend fun update(blogEntryId: Int, time: Instant) {
        if (!isFresh(time)) {
            storage.update {
                if (it == null || it.creationTime < time) CodeforcesLostHint(blogEntryId, time)
                else it
            }
        }
    }
}