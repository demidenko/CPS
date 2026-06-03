package com.demich.cps.platforms.codeforces.lost

import com.demich.cps.platforms.api.codeforces.models.CodeforcesBlogEntry
import com.demich.cps.platforms.utils.codeforces.CodeforcesColorTag
import com.demich.cps.platforms.utils.codeforces.CodeforcesRecentFeedBlogEntry
import kotlin.time.Clock

internal fun CodeforcesRecentFeedBlogEntry.toSuspect(): CodeforcesLostBlogEntrySuspect =
    CodeforcesLostBlogEntrySuspect(
        blogEntryId = id,
        authorColorTag = author.colorTag.takeIf { it == ADMIN }
    )

internal fun CodeforcesBlogEntry.toFresh(authorColorTag: CodeforcesColorTag?): CodeforcesLostBlogEntryFresh =
    CodeforcesLostBlogEntryFresh(
        blogEntry = this,
        authorColorTag = authorColorTag
    )

internal fun CodeforcesLostBlogEntryFresh.toLost(): CodeforcesLostBlogEntry =
    CodeforcesLostBlogEntry(
        blogEntry = blogEntry,
        authorColorTag = authorColorTag,
        timeStamp = Clock.System.now()
    )

internal fun CodeforcesLostBlogEntry.toFresh(): CodeforcesLostBlogEntryFresh =
    CodeforcesLostBlogEntryFresh(
        blogEntry = blogEntry,
        authorColorTag = authorColorTag
    )