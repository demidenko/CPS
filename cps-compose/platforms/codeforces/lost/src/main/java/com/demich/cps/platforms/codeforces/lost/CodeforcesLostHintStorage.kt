package com.demich.cps.platforms.codeforces.lost

import kotlinx.serialization.Serializable
import kotlin.time.Instant

abstract class CodeforcesLostHintStorage {
    abstract suspend fun getHint(): CodeforcesLostHint?

    protected abstract suspend fun update(transform: (CodeforcesLostHint?) -> CodeforcesLostHint)

    abstract suspend fun reset()

    suspend fun update(blogEntryId: Int, time: Instant) {
        update {
            if (it == null || it.creationTime < time) CodeforcesLostHint(blogEntryId, time)
            else it
        }
    }
}

@Serializable
data class CodeforcesLostHint(
    val blogEntryId: Int,
    val creationTime: Instant
)