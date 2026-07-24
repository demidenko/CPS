package com.demich.cps.platforms.utils.codeforces

import com.demich.cps.platforms.utils.href
import com.demich.cps.platforms.utils.parseDocument
import com.demich.cps.platforms.utils.selectSequence
import com.demich.cps.platforms.utils.values
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class CodeforcesRecentFeedPageParser: CodeforcesCommunityPageParser() {
    private fun Element.extractRecentBlogEntry(): CodeforcesRecentFeedBlogEntry {
        val author = expectRatedUser().extractRatedUser()
        val blogEntryId: Int
        val blogEntryTitle: String
        expectBlogEntryHref().let {
            blogEntryId = it.href.extractBlogEntryIdFromBlogEntryHref()
            blogEntryTitle = it.text()
        }

        return CodeforcesRecentFeedBlogEntry(
            id = blogEntryId,
            title = blogEntryTitle,
            author = author,
            isLowRated = false
        )
    }

    internal fun extractRecentFeedBlogEntries(document: Document): Sequence<Result<CodeforcesRecentFeedBlogEntry>> =
        document.expectSidebar().expectFirst("div.recent-actions")
            .selectSequence("li")
            .map { runCatching { it.extractRecentBlogEntry() } }

    fun parseRecentFeedBlogEntries(page: String): List<CodeforcesRecentFeedBlogEntry> =
        extractRecentFeedBlogEntries(page.parseDocument()).values().toList()
}