package com.demich.cps.platforms.codeforces.lost

import com.demich.cps.platforms.api.codeforces.models.CodeforcesBlogEntry
import com.demich.cps.platforms.utils.codeforces.CodeforcesUserTag
import kotlin.time.Instant

sealed interface CodeforcesLostEntry {
    val blogEntryId: Int
    val authorColorTag: CodeforcesUserTag?
}

data class CodeforcesLostBlogEntrySuspect(
    override val blogEntryId: Int,
    override val authorColorTag: CodeforcesUserTag?
): CodeforcesLostEntry

data class CodeforcesLostBlogEntryFresh(
    val blogEntry: CodeforcesBlogEntry,
    override val authorColorTag: CodeforcesUserTag?
): CodeforcesLostEntry {
    override val blogEntryId: Int
        get() = blogEntry.id
}

data class CodeforcesLostBlogEntry(
    val blogEntry: CodeforcesBlogEntry,
    override val authorColorTag: CodeforcesUserTag?,
    val timeStamp: Instant
): CodeforcesLostEntry {
    override val blogEntryId: Int
        get() = blogEntry.id
}