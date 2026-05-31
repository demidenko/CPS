package com.demich.cps.platforms.codeforces.lost

import com.demich.cps.platforms.api.codeforces.CodeforcesApi
import com.demich.cps.platforms.api.codeforces.CodeforcesPageContentProvider
import com.demich.cps.platforms.utils.codeforces.CodeforcesRecentFeedBlogEntry
import com.demich.cps.platforms.utils.codeforces.CodeforcesUtils
import kotlin.time.Instant

suspend fun CodeforcesLostStorage.updateEntries(
    api: CodeforcesApi,
    pageContentProvider: CodeforcesPageContentProvider,
    hintStorage: CodeforcesLostHintStorage,
    isFresh: (Instant) -> Boolean,
    isStale: (Instant) -> Boolean
) {
    val api = api.withBlogEntriesCache { blogEntry ->
        val time = blogEntry.creationTime
        if (!isFresh(time)) {
            //save hint
            hintStorage.update(blogEntry.id, time)
        }
    }

    val recentResult = pageContentProvider.getRecentCatching()

    recentResult.onFailure {
        val recentBlogEntries = api.getRecentActionsBlogEntries()

        recentBlogEntries.filter { isFresh(it.creationTime) }
        // TODO add to fresh
    }

    val recentBlogEntries = recentResult.getOrThrow()

    val hint = hintStorage.run {
        val hint = getHint()
        //ensure hint in case isFresh logic changes
        if (hint != null && isFresh(hint.creationTime)) {
            reset()
            null
        } else {
            hint
        }
    }

    updateSuspects(
        recent = recentBlogEntries,
        hint = hint
    )

    // TODO updateFresh

    // TODO updateLost
}

private suspend fun CodeforcesLostStorage.updateSuspects(
    recent: List<CodeforcesRecentFeedBlogEntry>,
    hint: CodeforcesLostHint?
) {
    val toAdd = recent.mapNotNull { blogEntry ->
        if (isNotFresh(blogEntry.id, hint)) null
        else CodeforcesLostBlogEntrySuspect(
            blogEntryId = blogEntry.id,
            authorColorTag = blogEntry.author.colorTag.takeIf { it == ADMIN }
        )
    }

    // TODO add to suspects
    // TODO filter not fresh suspects
}

private suspend fun CodeforcesLostStorage.updateFresh(

) {

}

private suspend fun CodeforcesLostStorage.updateLost(
    recent: List<CodeforcesRecentFeedBlogEntry>,
    api: CodeforcesApi,
    isStale: (Instant) -> Boolean
) {
    val recentIds = recent.map { it.id }

}

private suspend fun CodeforcesPageContentProvider.getRecentCatching(): Result<List<CodeforcesRecentFeedBlogEntry>> {
    suspend fun extractFrom(page: suspend () -> String) =
        CodeforcesUtils.extractRecentBlogEntries(source = page())

    // "/groups" has less size than "/recent" and hopefully will be cached by cf
    return runCatching {
        extractFrom(::getGroupsPage)
    }.recoverCatching {
        extractFrom(::getRecentActionsPage)
    }
}

private fun isNotFresh(blogEntryId: Int, hint: CodeforcesLostHint?) =
    hint != null && blogEntryId <= hint.blogEntryId