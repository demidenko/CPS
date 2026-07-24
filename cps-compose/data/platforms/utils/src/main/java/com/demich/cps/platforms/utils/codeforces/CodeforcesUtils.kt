package com.demich.cps.platforms.utils.codeforces

import com.demich.cps.platforms.api.codeforces.CodeforcesPageContentProvider
import com.demich.cps.platforms.utils.EvaluatorTagWithClass
import com.demich.cps.platforms.utils.expectFirst
import com.demich.cps.platforms.utils.href
import com.demich.cps.platforms.utils.parseDocument
import com.demich.cps.platforms.utils.parseHtmlElement
import com.demich.cps.platforms.utils.selectSequence
import com.demich.cps.platforms.utils.values
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Evaluator

private val evaluatorDivInfo = EvaluatorTagWithClass(tag = "div", className = "info")
private fun Element.expectDivInfo(): Element = expectFirst(evaluatorDivInfo)

private val evaluatorRatedUser = Evaluator.Class("rated-user")
private fun Element.selectRatedUser(): Element? = selectFirst(evaluatorRatedUser)
private fun Element.expectRatedUser(): Element = expectFirst(evaluatorRatedUser)

private val evaluatorHumanTime = Evaluator.Class("format-humantime")
private fun Element.expectHumanTime(): Element = expectFirst(evaluatorHumanTime)

private val evaluatorHrefBlogEntry = Evaluator.AttributeWithValueStarting("href", "/blog/entry/")


object CodeforcesUtils: CodeforcesPageParser {

    private fun extractRecentBlogEntry(item: Element): CodeforcesRecentFeedBlogEntry {
        val author = item.expectRatedUser().extractRatedUser()
        val blogEntryId: Int
        val blogEntryTitle: String
        item.expectFirst(evaluatorHrefBlogEntry).let {
            blogEntryId = it.href.removePrefix("/blog/entry/").toInt()
            blogEntryTitle = it.text()
        }

        return CodeforcesRecentFeedBlogEntry(
            id = blogEntryId,
            title = blogEntryTitle,
            author = author,
            isLowRated = false
        )
    }

    internal fun extractRecentBlogEntries(document: Document): Sequence<Result<CodeforcesRecentFeedBlogEntry>> =
        document.expectSidebar().expectFirst("div.recent-actions")
            .selectSequence("li")
            .map { runCatching { extractRecentBlogEntry(it) } }

    fun extractRecentBlogEntries(source: String): List<CodeforcesRecentFeedBlogEntry> =
        extractRecentBlogEntries(source.parseDocument()).values().toList()
}


suspend fun CodeforcesPageContentProvider.getRealColorTagOrNull(handle: String): CodeforcesColorTag? {
    val page = runCatching { getUserPage(handle) }.getOrElse { return null }
    return page.parseDocument()
        .selectFirst("div.userbox")
        ?.selectRatedUser()
        ?.extractRatedUser()
        ?.colorTag
}

suspend fun CodeforcesPageContentProvider.getHandleSuggestions(str: String): Sequence<CodeforcesHandle> =
    getHandleSuggestionsPage(str)
        .splitToSequence('\n')
        .filter { it.isNotEmpty() }
        .mapNotNull {
            val i = it.lastIndexOf('|')
            it.substring(i + 1).parseHtmlElement()
                .selectRatedUser()
                ?.extractRatedUser()
        }

