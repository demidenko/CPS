package com.demich.cps.platforms.codeforces.lost

import com.demich.cps.platforms.api.codeforces.models.CodeforcesBlogEntry
import kotlinx.serialization.Serializable

@Serializable
data class CodeforcesLostSuspect(
    val blogEntryId: Int,
    val authorHandle: String,
    val blogEntry: CodeforcesBlogEntry?
)