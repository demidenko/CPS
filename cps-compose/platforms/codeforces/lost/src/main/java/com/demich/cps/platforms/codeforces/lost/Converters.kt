package com.demich.cps.platforms.codeforces.lost

import com.demich.cps.platforms.api.codeforces.models.CodeforcesBlogEntry
import com.demich.cps.platforms.utils.codeforces.CodeforcesColorTag
import com.demich.cps.platforms.utils.codeforces.CodeforcesRecentFeedBlogEntry
import kotlin.time.Clock

internal fun CodeforcesRecentFeedBlogEntry.toSuspect(
    trustColorTag: Boolean
): CodeforcesLostBlogEntrySuspect =
    CodeforcesLostBlogEntrySuspect(
        blogEntryId = id,
        authorColorTag = author.colorTag.takeIf { trustColorTag || it == ADMIN }
    )

internal fun CodeforcesBlogEntry.toFresh(authorColorTag: CodeforcesColorTag?): CodeforcesLostBlogEntryFresh =
    CodeforcesLostBlogEntryFresh(
        blogEntry = this,
        authorColorTag = authorColorTag
    )

internal fun CodeforcesLostBlogEntryFresh.toLost(): CodeforcesLostBlogEntry =
    CodeforcesLostBlogEntry(
        blogEntry = blogEntry.copy(rating = 0),
        authorColorTag = authorColorTag,
        timeStamp = Clock.System.now()
    )

internal fun CodeforcesLostBlogEntry.toFresh(): CodeforcesLostBlogEntryFresh =
    CodeforcesLostBlogEntryFresh(
        blogEntry = blogEntry,
        authorColorTag = authorColorTag
    )