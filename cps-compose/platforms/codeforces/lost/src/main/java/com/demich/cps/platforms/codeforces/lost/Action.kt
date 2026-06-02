package com.demich.cps.platforms.codeforces.lost

import com.demich.cps.platforms.api.codeforces.CodeforcesApi
import com.demich.cps.platforms.api.codeforces.CodeforcesPageContentProvider
import com.demich.cps.platforms.api.codeforces.getRecentActions
import com.demich.cps.platforms.api.codeforces.models.CodeforcesBlogEntry
import com.demich.cps.platforms.utils.codeforces.CodeforcesColorTag
import com.demich.cps.platforms.utils.codeforces.CodeforcesRecentFeedBlogEntry
import com.demich.cps.platforms.utils.codeforces.CodeforcesUtils
import com.demich.cps.platforms.utils.codeforces.getUsersCatching
import kotlin.time.Clock
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
        val blogEntries = api.getRecentActionsBlogEntries()
            .mapNotNull {
                if (isFresh(it.creationTime)) {
                    CodeforcesLostBlogEntryFresh(
                        blogEntry = it,
                        authorColorTag = null
                    )
                } else {
                    null
                }
            }

        addFresh(blogEntries)
    }

    val recentBlogEntries = recentResult.getOrThrow()

    val hint = hintStorage.run {
        val hint = getHint()
        // ensure hint in case isFresh logic changes
        if (hint != null && isFresh(hint.creationTime)) {
            reset()
            null
        } else {
            hint
        }
    }

    updateEntries(
        api = api,
        recent = recentBlogEntries,
        hint = hint,
        isFresh = { isFresh(it.creationTime) },
        isStale = { isStale(it.creationTime) }
    )
}

private suspend fun CodeforcesLostStorage.updateEntries(
    api: CodeforcesApi,
    recent: List<CodeforcesRecentFeedBlogEntry>,
    hint: CodeforcesLostHint?,
    isFresh: (CodeforcesBlogEntry) -> Boolean,
    isStale: (CodeforcesBlogEntry) -> Boolean
) {
    updateSuspects(
        recent = recent,
        hint = hint
    )

    updateFresh(
        api = api,
        isFresh = isFresh
    )

    updateLost(
        recent = recent,
        api = api,
        isFresh = isFresh,
        isStale = isStale
    )
}

private suspend fun CodeforcesLostStorage.updateSuspects(
    recent: List<CodeforcesRecentFeedBlogEntry>,
    hint: CodeforcesLostHint?
) {
    editEntries {
        entries.removeAll { (id, it) ->
            it is CodeforcesLostBlogEntrySuspect && isNotFresh(id, hint)
        }

        recent.forEach { blogEntry ->
            if (!isNotFresh(blogEntry.id, hint) && blogEntry.id !in this) {
                val suspect = CodeforcesLostBlogEntrySuspect(
                    blogEntryId = blogEntry.id,
                    authorColorTag = blogEntry.author.colorTag.takeIf { it == ADMIN }
                )
                put(suspect)
            }
        }
    }
}

private suspend fun CodeforcesLostStorage.addFresh(
    blogEntries: List<CodeforcesLostBlogEntryFresh>
) {
    if (blogEntries.isEmpty()) return

    editEntries {
        blogEntries.forEach {
            if (it.blogEntryId !in this) put(it)
        }
    }
}

private suspend fun CodeforcesLostStorage.updateFresh(
    api: CodeforcesApi,
    isFresh: (CodeforcesBlogEntry) -> Boolean
) {
    editEntries {
        values.removeAll {
            it is CodeforcesLostBlogEntryFresh && !isFresh(it.blogEntry)
        }
    }

    val suspects = getEntriesOf<CodeforcesLostBlogEntrySuspect>()
        .sortedBy { it.blogEntryId }

    if (suspects.size > 1) {
        api.getRecentActions()
    }

    suspects.forEach { suspect ->
        val blogEntry = api.getBlogEntryOrNull(blogEntryId = suspect.blogEntryId)

        if (blogEntry != null && isFresh(blogEntry)) {
            editEntries {
                val fresh = CodeforcesLostBlogEntryFresh(
                    blogEntry = blogEntry,
                    authorColorTag = suspect.authorColorTag
                )
                put(fresh)
            }
        } else {
            editEntries {
                remove(suspect.blogEntryId)
            }
        }
    }
}

private suspend fun CodeforcesLostStorage.updateLost(
    recent: List<CodeforcesRecentFeedBlogEntry>,
    api: CodeforcesApi,
    isFresh: (CodeforcesBlogEntry) -> Boolean,
    isStale: (CodeforcesBlogEntry) -> Boolean
) {
    val recentIds = recent.map { it.id }

    editEntries {
        entries.replaceValues { (id, it) ->
            when {
                it !is CodeforcesLostBlogEntry -> it
                isStale(it.blogEntry) -> null
                it.blogEntryId in recentIds -> {
                    if (isFresh(it.blogEntry)) {
                        CodeforcesLostBlogEntryFresh(
                            blogEntry = it.blogEntry,
                            authorColorTag = it.authorColorTag
                        )
                    } else {
                        null
                    }
                }
                else -> it
            }
        }
    }

    getEntriesOf<CodeforcesLostBlogEntryFresh>()
        .filter { it.blogEntryId !in recentIds }
        .let { toLost ->
            if (toLost.isNotEmpty()) {
                editEntries {
                    toLost.forEach {
                        val lost = CodeforcesLostBlogEntry(
                            blogEntry = it.blogEntry,
                            authorColorTag = it.authorColorTag,
                            timeStamp = Clock.System.now()
                        )
                        put(lost)
                    }
                }
            }
        }

    val lost = getEntriesOf<CodeforcesLostBlogEntry>()

    if (lost.any { it.authorColorTag == null }) {
        // TODO: use this to get colortags for all entries in storage
        val users = api.getUsersCatching(
            handles = lost.mapNotNull {
                if (it.authorColorTag == null) it.blogEntry.authorHandle
                else null
            },
            checkHistoricHandles = false
        )

        editNullColorTags(
            colorTag = {
                users[it.blogEntry.authorHandle]
                    ?.getOrNull()
                    ?.let { user -> CodeforcesColorTag.fromRating(user.rating) }
            }
        )
    }
}

private suspend fun CodeforcesLostStorage.editNullColorTags(
    colorTag: (CodeforcesLostBlogEntry) -> CodeforcesColorTag?
) {
    editEntries {
        entries.forEach { mapEntry ->
            val it = mapEntry.value
            if (it is CodeforcesLostBlogEntry && it.authorColorTag == null) {
                val colorTag = colorTag(it)
                if (colorTag != null) {
                    mapEntry.setValue(it.copy(authorColorTag = colorTag))
                }
            }
        }
    }
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

private fun <K, V: Any> MutableCollection<MutableMap.MutableEntry<K, V>>.replaceValues(
    transform: (Map.Entry<K, V>) -> V?
) {
    removeAll {
        val newValue = transform(it)
        if (newValue == null) true
        else {
            it.setValue(newValue)
            false
        }
    }
}