package com.demich.cps.platforms.codeforces.lost

import com.demich.cps.platforms.api.codeforces.models.CodeforcesBlogEntry
import com.demich.cps.platforms.utils.codeforces.CodeforcesColorTag
import kotlin.time.Instant

sealed interface CodeforcesLostEntry {
    val blogEntryId: Int
    val authorColorTag: CodeforcesColorTag?
}

data class CodeforcesLostBlogEntrySuspect(
    override val blogEntryId: Int,
    override val authorColorTag: CodeforcesColorTag?
): CodeforcesLostEntry

data class CodeforcesLostBlogEntryFresh(
    val blogEntry: CodeforcesBlogEntry,
    override val authorColorTag: CodeforcesColorTag?
): CodeforcesLostEntry {
    override val blogEntryId: Int
        get() = blogEntry.id
}

data class CodeforcesLostBlogEntry(
    val blogEntry: CodeforcesBlogEntry,
    override val authorColorTag: CodeforcesColorTag?,
    val timeStamp: Instant
): CodeforcesLostEntry {
    override val blogEntryId: Int
        get() = blogEntry.id
}