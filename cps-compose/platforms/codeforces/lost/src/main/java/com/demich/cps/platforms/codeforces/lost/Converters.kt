package com.demich.cps.platforms.codeforces.lost

import com.demich.cps.platforms.api.codeforces.models.CodeforcesBlogEntry
import com.demich.cps.platforms.utils.codeforces.CodeforcesAdminTag
import com.demich.cps.platforms.utils.codeforces.CodeforcesRecentFeedBlogEntry
import com.demich.cps.platforms.utils.codeforces.CodeforcesUserTag
import kotlin.time.Clock

internal fun CodeforcesRecentFeedBlogEntry.toSuspect(
    trustColorTag: Boolean
): CodeforcesLostBlogEntrySuspect =
    CodeforcesLostBlogEntrySuspect(
        blogEntryId = id,
        authorUserTag = author.userTag.takeIf { trustColorTag || it is CodeforcesAdminTag }
    )

internal fun CodeforcesBlogEntry.toFresh(authorUserTag: CodeforcesUserTag?): CodeforcesLostBlogEntryFresh =
    CodeforcesLostBlogEntryFresh(
        blogEntry = this.copy(rating = 0),
        authorUserTag = authorUserTag
    )

internal fun CodeforcesLostBlogEntryFresh.toLost(): CodeforcesLostBlogEntry =
    CodeforcesLostBlogEntry(
        blogEntry = blogEntry,
        authorUserTag = authorUserTag,
        timeStamp = Clock.System.now()
    )

internal fun CodeforcesLostBlogEntry.toFresh(): CodeforcesLostBlogEntryFresh =
    CodeforcesLostBlogEntryFresh(
        blogEntry = blogEntry,
        authorUserTag = authorUserTag
    )