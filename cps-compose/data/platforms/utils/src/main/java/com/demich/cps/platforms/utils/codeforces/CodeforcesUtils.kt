package com.demich.cps.platforms.utils.codeforces

import com.demich.cps.platforms.api.codeforces.CodeforcesPageContentProvider
import com.demich.cps.platforms.utils.EvaluatorNthTag
import com.demich.cps.platforms.utils.EvaluatorTagWithClass
import com.demich.cps.platforms.utils.expectFirst
import com.demich.cps.platforms.utils.selectSequence
import com.demich.cps.platforms.utils.values
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.alternativeParsing
import kotlinx.datetime.format.char
import kotlinx.datetime.toInstant
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Evaluator
import kotlin.time.Instant

private fun Document.expectContent(): Element = expectFirst("div.content-with-sidebar")

private fun Document.selectSidebar(): Element? = selectFirst("div#sidebar")
private fun Document.expectSidebar(): Element = requireNotNull(selectSidebar())

private val evaluatorDivInfo = EvaluatorTagWithClass(tag = "div", className = "info")
private fun Element.expectDivInfo(): Element = expectFirst(evaluatorDivInfo)

private val evaluatorRatedUser = Evaluator.Class("rated-user")
private fun Element.selectRatedUser(): Element? = selectFirst(evaluatorRatedUser)
private fun Element.expectRatedUser(): Element = expectFirst(evaluatorRatedUser)

private val evaluatorHumanTime = Evaluator.Class("format-humantime")
private fun Element.expectHumanTime(): Element = expectFirst(evaluatorHumanTime)

private val evaluatorHrefBlogEntry = Evaluator.AttributeWithValueStarting("href", "/blog/entry/")

private object CodeforcesDateTimeParser {
    private val moscowTimeZone = kotlinx.datetime.TimeZone.of("Europe/Moscow")

    private val dateTimeFormat = LocalDateTime.Format {
        alternativeParsing({
            //RU format: "dd.MM.yyyy HH:mm"
            day()
            char('.')
            monthNumber()
            char('.')
            year()
        }) {
            //EN format: "MMM/dd/yyyy HH:mm"
            monthName(MonthNames.ENGLISH_ABBREVIATED)
            char('/')
            day()
            char('/')
            year()
        }
        char(' ')
        hour()
        char(':')
        minute()
    }

    fun parse(input: String): Instant =
        LocalDateTime.parse(input, dateTimeFormat).toInstant(moscowTimeZone)
}

private fun Element.extractTime(): Instant = CodeforcesDateTimeParser.parse(attr("title"))


object CodeforcesUtils {

    private val evaluatorDivTitle = EvaluatorTagWithClass(tag = "div", className = "title")
    private val evaluatorMeta = Evaluator.Class("meta")
    private val evaluatorLeftMeta = Evaluator.Class("left-meta")
    private val evaluatorRightMeta = Evaluator.Class("right-meta")
    private val evaluatorTopicRating = Evaluator.Class("topic-rating")
    private val evaluator_li2 = EvaluatorNthTag("li", 2)
    private val evaluator_a1 = EvaluatorNthTag("a", 1)
    private fun extractBlogEntry(topic: Element): CodeforcesWebBlogEntry {
        val id: Int
        val title: String
        topic.expectFirst(evaluatorDivTitle).expectFirst("a").let {
            title = it.text()
            id = it.attr("href").removePrefix("/blog/entry/").toInt()
        }

        val author: CodeforcesHandle
        val creationTime: Instant
        topic.expectDivInfo().let { info ->
            author = info.expectRatedUser().extractRatedUser()
            creationTime = info.expectHumanTime().extractTime()
        }

        val rating: Int
        val commentsCount: Int
        topic.expectFirst(evaluatorMeta).let { bottom ->
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
                commentLink.attr("href").let { url ->
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
            blogEntryId = it.attr("href").removePrefix("/blog/entry/").toInt()
            blogEntryTitle = it.text()
        }

        return CodeforcesRecentFeedBlogEntry(
            id = blogEntryId,
            title = blogEntryTitle,
            author = author,
            isLowRated = false
        )
    }


    internal fun extractBlogEntries(document: Document): Sequence<Result<CodeforcesWebBlogEntry>> =
        document.expectContent().selectSequence("div.topic")
            .map { runCatching { extractBlogEntry(it) } }

    fun extractBlogEntries(source: String): List<CodeforcesWebBlogEntry> =
        extractBlogEntries(Jsoup.parse(source)).values().toList()

    internal fun extractComments(document: Document): Sequence<Result<CodeforcesWebComment>> =
        document.expectContent().selectSequence(".comment-table")
            .map { runCatching { extractComment(it) } }

    fun extractComments(source: String): List<CodeforcesWebComment> =
        extractComments(Jsoup.parse(source)).values().toList()

    internal fun extractRecentBlogEntries(document: Document): Sequence<Result<CodeforcesRecentFeedBlogEntry>> =
        document.expectSidebar().expectFirst("div.recent-actions")
            .selectSequence("li")
            .map { runCatching { extractRecentBlogEntry(it) } }

    fun extractRecentBlogEntries(source: String): List<CodeforcesRecentFeedBlogEntry> =
        extractRecentBlogEntries(Jsoup.parse(source)).values().toList()

    private inline fun extractContestPhaseInfo(source: String, block: (String, String) -> Unit) {
        val sidebar = Jsoup.parse(source).selectSidebar() ?: return
        val phaseText = sidebar.selectFirst("span.contest-state-phase")?.text() ?: return
        val infoText = sidebar.selectFirst("span.contest-state-regular")?.text() ?: return
        block(phaseText, infoText)
    }

    fun extractContestSystemTestingPercentageOrNull(source: String): Int? {
        extractContestPhaseInfo(source) { phase, text ->
            if (phase != "System testing") return null
            return text.removeSuffix("%").toIntOrNull()
        }
        return null
    }
}


suspend fun CodeforcesPageContentProvider.getRealColorTagOrNull(handle: String): CodeforcesColorTag? {
    val page = runCatching { getUserPage(handle) }.getOrElse { return null }
    return Jsoup.parse(page).selectFirst("div.userbox")
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
            Jsoup.parse(it.substring(i + 1))
                .selectRatedUser()
                ?.extractRatedUser()
        }

suspend fun CodeforcesPageContentProvider.getSysTestPercentageOrNull(contestId: Int): Int? =
    runCatching { getContestPage(contestId = contestId) }
        .map { CodeforcesUtils.extractContestSystemTestingPercentageOrNull(it) }
        .getOrNull()