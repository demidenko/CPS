package com.demich.cps.platforms.codeforces.lost

import com.demich.cps.platforms.api.codeforces.CodeforcesApi
import com.demich.cps.platforms.api.codeforces.CodeforcesPageContentProvider
import com.demich.cps.platforms.api.codeforces.getRecentActions
import com.demich.cps.platforms.api.codeforces.models.CodeforcesBlogEntry
import com.demich.cps.platforms.utils.codeforces.CodeforcesColorTag
import com.demich.cps.platforms.utils.codeforces.CodeforcesRecentFeedBlogEntry
import com.demich.cps.platforms.utils.codeforces.CodeforcesUtils
import com.demich.cps.platforms.utils.codeforces.getUsersCatching
import kotlin.time.Instant

suspend fun CodeforcesLostStorage.updateEntries(
    api: CodeforcesApi,
    pageContentProvider: CodeforcesPageContentProvider,
    hintStorage: CodeforcesLostHintStorage,
    isFresh: (Instant) -> Boolean,
    isStale: (Instant) -> Boolean
) {
    val hintStorage = CheckedHintStorage(
        storage = hintStorage,
        isFresh = isFresh
    )

    val delayedStorage = DelayedTransactionsLostStorage(this)

    val api = api
        .withBeforeGetCall { delayedStorage.pushChanges() }
        .withBlogEntriesCache { blogEntry ->
            hintStorage.update(blogEntry.id, blogEntry.creationTime)
        }

    val recentResult = pageContentProvider.getRecentCatching()

    recentResult.onFailure {
        val blogEntries = api.getRecentActionsBlogEntries()
            .mapNotNull {
                if (isFresh(it.creationTime)) {
                    it.toFresh(authorColorTag = null)
                } else {
                    null
                }
            }

        addFresh(blogEntries)
    }

    delayedStorage.use {
        updateEntries(
            api = api,
            recent = recentResult.getOrThrow(),
            hint = hintStorage.getHint(),
            isFresh = { isFresh(it.creationTime) },
            isStale = { isStale(it.creationTime) }
        )
    }
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
                put(blogEntry.toSuspect())
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
        api.runCatching { getRecentActions() }
    }

    suspects.forEach { suspect ->
        val blogEntry = api.getBlogEntryOrNull(blogEntryId = suspect.blogEntryId)

        if (blogEntry != null && isFresh(blogEntry)) {
            editEntries {
                put(blogEntry.toFresh(authorColorTag = suspect.authorColorTag))
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
                        it.toFresh()
                    } else {
                        null
                    }
                }
                else -> it
            }
        }

        // fresh become lost
        entries.replaceValues { (id, it) ->
            if (it is CodeforcesLostBlogEntryFresh && it.blogEntryId !in recentIds) {
                it.toLost()
            } else {
                it
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

private class DelayedTransactionsLostStorage(
    private val origin: CodeforcesLostStorage
): CodeforcesLostStorage {
    private typealias UpdateLambda = (Map<Int, CodeforcesLostEntry>) -> Map<Int, CodeforcesLostEntry>
    private var blocks = mutableListOf<UpdateLambda>()

    override suspend fun update(transform: UpdateLambda) {
        blocks.add(transform)
    }

    override suspend fun getEntries(): Map<Int, CodeforcesLostEntry> {
        pushChanges()
        return origin.getEntries()
    }

    suspend fun pushChanges() {
        if (blocks.isNotEmpty()) {
            origin.update {
                blocks.fold(initial = it) { map, block -> block(map) }
            }
            blocks.clear()
        }
    }
}

private suspend inline fun DelayedTransactionsLostStorage.use(block: DelayedTransactionsLostStorage.() -> Unit) {
    try {
        block()
    } finally {
        pushChanges()
    }
}