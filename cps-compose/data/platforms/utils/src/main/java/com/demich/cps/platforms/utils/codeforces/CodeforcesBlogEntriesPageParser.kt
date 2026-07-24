package com.demich.cps.platforms.utils.codeforces

import com.demich.cps.platforms.utils.EvaluatorNthTag
import com.demich.cps.platforms.utils.EvaluatorTagWithClass
import com.demich.cps.platforms.utils.expectFirst
import com.demich.cps.platforms.utils.href
import com.demich.cps.platforms.utils.parseDocument
import com.demich.cps.platforms.utils.selectSequence
import com.demich.cps.platforms.utils.values
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Evaluator
import kotlin.time.Instant

class CodeforcesBlogEntriesPageParser: CodeforcesCommunityPageParser() {
    private val evaluatorDivTitle = EvaluatorTagWithClass(tag = "div", className = "title")
    private val evaluatorMeta = Evaluator.Class("meta")
    private val evaluatorLeftMeta = Evaluator.Class("left-meta")
    private val evaluatorRightMeta = Evaluator.Class("right-meta")
    private val evaluatorTopicRating = Evaluator.Class("topic-rating")
    private val evaluator_li2 = EvaluatorNthTag("li", 2)
    private val evaluator_a1 = EvaluatorNthTag("a", 1)

    private fun Element.extractBlogEntry(): CodeforcesWebBlogEntry {
        val id: Int
        val title: String
        expectFirst(evaluatorDivTitle).expectFirst("a").let {
            title = it.text()
            id = it.href.extractBlogEntryIdFromBlogEntryHref()
        }

        val author: CodeforcesHandle
        val creationTime: Instant
        expectDivInfo().let { info ->
            author = info.expectRatedUser().extractRatedUser()
            creationTime = info.expectHumanTime().extractTime()
        }

        val rating: Int
        val commentsCount: Int
        expectFirst(evaluatorMeta).let { bottom ->
            rating = bottom.expectFirst(evaluatorLeftMeta).expectFirst(evaluatorTopicRating).text().toInt()
            val commentsItem = bottom.expectFirst(evaluatorRightMeta).expectFirst(evaluator_li2)
            commentsCount = commentsItem.expectFirst(evaluator_a1).text().toInt()
        }

        return CodeforcesWebBlogEntry(
            id = id,
            title = title,
            author = author,
            creationTime = creationTime,
            rating = rating,
            commentsCount = commentsCount
        )
    }

    internal fun extractBlogEntries(document: Document): Sequence<Result<CodeforcesWebBlogEntry>> =
        document.expectContent().selectSequence("div.topic")
            .map { runCatching { it.extractBlogEntry() } }

    fun parseBlogEntries(page: String): List<CodeforcesWebBlogEntry> =
        extractBlogEntries(page.parseDocument()).values().toList()
}