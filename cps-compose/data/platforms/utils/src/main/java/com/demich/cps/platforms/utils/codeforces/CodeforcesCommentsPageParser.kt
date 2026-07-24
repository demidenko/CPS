package com.demich.cps.platforms.utils.codeforces

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

class CodeforcesCommentsPageParser: CodeforcesCommunityPageParser() {
    private val evaluatorAvatar = Evaluator.Class("avatar")
    private val evaluatorAttrCommentId = Evaluator.Attribute("commentid")
    private val evaluatorDivTypography = EvaluatorTagWithClass(tag = "div", className = "ttypography")

    private fun Element.extractComment(): CodeforcesWebComment {
        val commentator = expectFirst(evaluatorAvatar)
            .expectRatedUser().extractRatedUser()

        val blogEntryId: Int
        val blogEntryTitle: String
        val blogEntryAuthor: CodeforcesHandle
        val commentId: Long
        val commentCreationTime: Instant
        val commentRating: Int
        expectDivInfo().let { info ->
            blogEntryAuthor = info.expectRatedUser().extractRatedUser()
            commentCreationTime = info.expectHumanTime().extractTime()
            info.expectBlogEntryHref().let { commentLink ->
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
        val commentHtml = selectFirst(evaluatorDivTypography)?.html().orEmpty()

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


    internal fun extractComments(document: Document): Sequence<Result<CodeforcesWebComment>> =
        document.expectContent().selectSequence(".comment-table")
            .map { runCatching { it.extractComment() } }

    fun parseComments(page: String): List<CodeforcesWebComment> =
        extractComments(page.parseDocument()).values().toList()
}