package com.demich.cps.platforms.codeforces.lost

import com.demich.cps.platforms.api.codeforces.CodeforcesApi
import com.demich.cps.platforms.api.codeforces.CodeforcesApiBlogEntryNotFoundException
import com.demich.cps.platforms.api.codeforces.models.CodeforcesBlogEntry

internal fun CodeforcesApi.withBlogEntriesCache(
    onNewBlogEntry: suspend (CodeforcesBlogEntry) -> Unit
): CodeforcesApi {
    val origin = this
    return object : CodeforcesApi by origin {
        private val cache = mutableMapOf<Int, CodeforcesBlogEntry>()

        private suspend inline fun getOrPut(id: Int, blogEntry: () -> CodeforcesBlogEntry): CodeforcesBlogEntry =
            cache.getOrPut(key = id) {
                blogEntry().also { onNewBlogEntry(it) }
            }

        override suspend fun getBlogEntry(blogEntryId: Int) =
            getOrPut(id = blogEntryId) {
                origin.getBlogEntry(blogEntryId)
            }

        override suspend fun getRecentActions(maxCount: Int) =
            origin.getRecentActions(maxCount = maxCount).apply {
                forEach {
                    it.blogEntry?.let { blogEntry ->
                        getOrPut(id = blogEntry.id) { blogEntry }
                    }
                }
            }
    }
}

// null only if blog entry not found!
internal suspend fun CodeforcesApi.getBlogEntryOrNull(blogEntryId: Int): CodeforcesBlogEntry? {
    return try {
        getBlogEntry(blogEntryId = blogEntryId)
    } catch (e: CodeforcesApiBlogEntryNotFoundException) {
        null
    }
}