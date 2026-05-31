package com.demich.cps.platforms.codeforces.lost

import com.demich.cps.platforms.api.codeforces.models.CodeforcesBlogEntry
import com.demich.cps.platforms.utils.codeforces.CodeforcesColorTag
import kotlin.time.Instant

sealed interface CodeforcesLostEntry {
    val blogEntryId: Int
}

data class CodeforcesLostBlogEntrySuspect(
    override val blogEntryId: Int,
    val authorColorTag: CodeforcesColorTag?
): CodeforcesLostEntry

data class CodeforcesLostBlogEntryFresh(
    val blogEntry: CodeforcesBlogEntry,
    val authorColorTag: CodeforcesColorTag?
): CodeforcesLostEntry {
    override val blogEntryId: Int
        get() = blogEntry.id
}

data class CodeforcesLostBlogEntry(
    val blogEntry: CodeforcesBlogEntry,
    val authorColorTag: CodeforcesColorTag,
    val timeStamp: Instant
): CodeforcesLostEntry {
    override val blogEntryId: Int
        get() = blogEntry.id
}