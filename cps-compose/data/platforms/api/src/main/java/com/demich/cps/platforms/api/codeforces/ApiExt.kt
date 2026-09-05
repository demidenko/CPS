package com.demich.cps.platforms.api.codeforces

import com.demich.cps.platforms.api.codeforces.models.CodeforcesBlogEntry
import com.demich.cps.platforms.api.codeforces.models.CodeforcesUser

// returns null if user not found
suspend fun CodeforcesApi.getUserOrNull(
    handle: String,
    checkHistoricHandles: Boolean
): CodeforcesUser? =
    try {
        getUser(handle = handle, checkHistoricHandles = checkHistoricHandles)
    } catch (_: CodeforcesApiUserNotFoundException) {
        // TODO: check(it.handle == handle) ???
        null
    }

// returns null if blog entry not found
suspend fun CodeforcesApi.getBlogEntryOrNull(blogEntryId: Int): CodeforcesBlogEntry? =
    try {
        getBlogEntry(blogEntryId = blogEntryId)
    } catch (_: CodeforcesApiBlogEntryNotFoundException) {
        null
    }

suspend fun CodeforcesApi.getUserBlogEntriesRecovered(handle: String): List<CodeforcesBlogEntry> =
    try {
        getUserBlogEntries(handle = handle)
    } catch (_: CodeforcesApiBlogReadNotAllowedException) {
        emptyList()
    }