package com.demich.cps.platforms.codeforces.lost

import com.demich.cps.platforms.api.codeforces.models.CodeforcesBlogEntry
import com.demich.cps.platforms.utils.codeforces.CodeforcesAdminTag
import com.demich.cps.platforms.utils.codeforces.CodeforcesRecentFeedBlogEntry
import com.demich.cps.platforms.utils.codeforces.CodeforcesUserTag
import com.demich.cps.platforms.utils.codeforces.userTag
import kotlin.time.Clock

internal fun CodeforcesRecentFeedBlogEntry.toSuspect(
    trustColorTag: Boolean
): CodeforcesLostBlogEntrySuspect =
    CodeforcesLostBlogEntrySuspect(
        blogEntryId = id,
        authorColorTag = author.userTag.takeIf { trustColorTag || it is CodeforcesAdminTag }
    )

internal fun CodeforcesBlogEntry.toFresh(authorColorTag: CodeforcesUserTag?): CodeforcesLostBlogEntryFresh =
    CodeforcesLostBlogEntryFresh(
        blogEntry = this.copy(rating = 0),
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