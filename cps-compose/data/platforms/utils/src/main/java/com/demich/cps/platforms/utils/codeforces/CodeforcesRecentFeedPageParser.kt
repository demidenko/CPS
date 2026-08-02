package com.demich.cps.platforms.utils.codeforces

import com.demich.cps.platforms.utils.EvaluatorTagWithClass
import com.demich.cps.platforms.utils.expectFirst
import com.demich.cps.platforms.utils.href
import com.demich.cps.platforms.utils.parseDocument
import com.demich.cps.platforms.utils.selectSequence
import com.demich.cps.platforms.utils.values
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class CodeforcesRecentFeedPageParser:
    CodeforcesRatedUserSelector by CodeforcesRatedUserSelectorImpl(),
    CodeforcesHrefBlogEntrySelector by CodeforcesHrefBlogEntrySelectorImpl()
{
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

    private fun Document.selectRecentFeedBlogEntries() =
        expectSidebar()
            .expectFirst(EvaluatorTagWithClass(tag = "div", className = "recent-actions"))
            .selectSequence("li")

    internal fun extractRecentFeedBlogEntries(document: Document): Sequence<Result<CodeforcesRecentFeedBlogEntry>> =
        document.selectRecentFeedBlogEntries()
            .map { runCatching { it.extractRecentBlogEntry() } }

    fun parseRecentFeedBlogEntries(page: String): List<CodeforcesRecentFeedBlogEntry> =
        extractRecentFeedBlogEntries(page.parseDocument()).values().toList()
}