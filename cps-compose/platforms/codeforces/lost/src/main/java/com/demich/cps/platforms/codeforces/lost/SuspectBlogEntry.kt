package com.demich.cps.platforms.codeforces.lost

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class CodeforcesLostSuspect(
    val blogEntryId: Int,
    val creationTime: Instant?,
    val authorHandle: String
)