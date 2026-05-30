package com.demich.cps.platforms.codeforces.lost

import com.demich.cps.platforms.api.codeforces.models.CodeforcesBlogEntry
import com.demich.cps.platforms.utils.codeforces.CodeforcesColorTag
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class CodeforcesLostBlogEntrySuspect(
    val blogEntryId: Int,
    val authorColorTag: CodeforcesColorTag?
)

@Serializable
data class CodeforcesLostBlogEntryFresh(
    val blogEntry: CodeforcesBlogEntry,
    val authorColorTag: CodeforcesColorTag?
)

@Serializable
data class CodeforcesLostBlogEntry(
    val blogEntry: CodeforcesBlogEntry,
    val authorColorTag: CodeforcesColorTag,
    val timeStamp: Instant
)