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
import kotlin.time.Instant

private val evaluatorDivInfo = EvaluatorTagWithClass(tag = "div", className = "info")
private fun Element.expectDivInfo(): Element = expectFirst(evaluatorDivInfo)

private val evaluatorRatedUser = Evaluator.Class("rated-user")
private fun Element.selectRatedUser(): Element? = selectFirst(evaluatorRatedUser)
private fun Element.expectRatedUser(): Element = expectFirst(evaluatorRatedUser)

private val evaluatorHumanTime = Evaluator.Class("format-humantime")
private fun Element.expectHumanTime(): Element = expectFirst(evaluatorHumanTime)

private val evaluatorHrefBlogEntry = Evaluator.AttributeWithValueStarting("href", "/blog/entry/")


object CodeforcesUtils: CodeforcesPageParser {



    private val evaluatorAvatar = Evaluator.Class("avatar")
    private val evaluatorAttrCommentId = Evaluator.Attribute("commentid")
    private val evaluatorDivTypography = EvaluatorTagWithClass(tag = "div", className = "ttypography")
    private fun extractComment(commentBox: Element): CodeforcesWebComment {
        val commentator = commentBox.expectFirst(evaluatorAvatar)
            .expectRatedUser()
            .extractRatedUser()

        val blogEntryId: Int
        val blogEntryTitle: String
        val blogEntryAuthor: CodeforcesHandle
        val commentId: Long
        val commentCreationTime: Instant
        val commentRating: Int
        commentBox.expectDivInfo().let { info ->
            blogEntryAuthor = info.expectRatedUser().extractRatedUser()
            commentCreationTime = info.expectHumanTime().extractTime()
            info.expectFirst(evaluatorHrefBlogEntry).let { commentLink ->
                blogEntryTitle = commentLink.text()
                commentLink.href.let { url ->
                    // href="/blog/entry/XXXXXX#comment-YYYYYY"
                    val j = url.indexOf('#')
                    val i = url.lastIndexOf('/', j - 1)
                    blogEntryId = url.substring(i + 1, j).toInt()
                }
            }
            info.expectFirst(evaluatorAttrCommentId).let { ratingBox ->
                commentId = ratingBox.attr("commentid").toLong()
                commentRating = ratingBox.text().toInt()
            }
        }

        //<span class="notice">Пользователь создал или обновил текст</span>
        //<span class="notice">Комментарий удален по причине нарушения правил Codeforces</span>
        //TODO: use outerHtml() to match api response
        val commentHtml = commentBox.selectFirst(evaluatorDivTypography)?.html().orEmpty()

        return CodeforcesWebComment(
            id = commentId,
            author = commentator,
            html = commentHtml,
            rating = commentRating,
            creationTime = commentCreationTime,
            blogEntryId = blogEntryId,
            blogEntryTitle = blogEntryTitle,
            blogEntryAuthor = blogEntryAuthor,
        )
    }

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

    internal fun extractComments(document: Document): Sequence<Result<CodeforcesWebComment>> =
        document.expectContent().selectSequence(".comment-table")
            .map { runCatching { extractComment(it) } }

    fun extractComments(source: String): List<CodeforcesWebComment> =
        extractComments(source.parseDocument()).values().toList()

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

