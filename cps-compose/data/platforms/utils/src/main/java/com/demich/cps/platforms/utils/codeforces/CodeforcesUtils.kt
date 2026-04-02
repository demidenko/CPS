package com.demich.cps.platforms.utils.codeforces

import com.demich.cps.platforms.api.codeforces.CodeforcesPageContentProvider
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.alternativeParsing
import kotlinx.datetime.format.char
import kotlinx.datetime.toInstant
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import kotlin.time.Instant

private fun Element.expectContent(): Element = expectFirst("div.content-with-sidebar")

private fun Element.selectSidebar(): Element? = selectFirst("div#sidebar")
private fun Element.expectSidebar(): Element = requireNotNull(selectSidebar())

private fun Element.expectDivInfo(): Element = expectFirst("div.info")

private fun Element.selectRatedUser(): Element? = selectFirst(".rated-user")
private fun Element.expectRatedUser(): Element = requireNotNull(selectRatedUser())

private fun Element.expectHumanTimeTitle(): String = expectFirst(".format-humantime").attr("title")

object CodeforcesUtils {
    private object DateTimeParser {
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

    private fun String.extractTime(): Instant = DateTimeParser.parse(this)

    private fun extractBlogEntryOrNull(topic: Element): CodeforcesWebBlogEntry? {
        return kotlin.runCatching {
            val id: Int
            val title: String
            topic.expectFirst("div.title").expectFirst("a").let {
                title = it.text()
                id = it.attr("href").removePrefix("/blog/entry/").toInt()
            }

            val author: CodeforcesHandle
            val creationTime: Instant
            topic.expectDivInfo().let { info ->
                author = info.expectRatedUser().extractRatedUser()
                creationTime = info.expectHumanTimeTitle().extractTime()
            }

            val rating: Int
            val commentsCount: Int
            topic.expectFirst(".roundbox").let { box ->
                rating = box.expectFirst(".left-meta").expectFirst("span").text().toInt()
                val commentsItem = box.expectFirst(".right-meta").select("li")[2]
                commentsCount = commentsItem.select("a")[1].text().toInt()
            }

            CodeforcesWebBlogEntry(
                id = id,
                title = title,
                author = author,
                creationTime = creationTime,
                rating = rating,
                commentsCount = commentsCount
            )
        }.getOrNull()
    }

    // TODO: to slow probably
    private fun extractCommentOrNull(commentBox: Element): CodeforcesWebComment? {
        return kotlin.runCatching {
            val commentator = commentBox.expectFirst(".avatar")
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
                commentCreationTime = info.expectHumanTimeTitle().extractTime()
                info.getElementsByAttributeValueContaining("href", "#comment")[0].let { commentLink ->
                    with(commentLink.attr("href").split("#comment-")) {
                        blogEntryId = this[0].removePrefix("/blog/entry/").toInt()
                        commentId = this[1].toLong()
                    }
                    blogEntryTitle = commentLink.text()
                }
                info.getElementsByAttribute("commentid")[0].let { ratingBox ->
                    commentRating = ratingBox.text().trim().toInt()
                }
            }

            //<span class="notice">Пользователь создал или обновил текст</span>
            //<span class="notice">Комментарий удален по причине нарушения правил Codeforces</span>
            //TODO: use outerHtml() to match api response
            val commentHtml = commentBox.selectFirst("div.ttypography")?.html()
                ?: ""

            CodeforcesWebComment(
                id = commentId,
                author = commentator,
                html = commentHtml,
                rating = commentRating,
                creationTime = commentCreationTime,
                blogEntryId = blogEntryId,
                blogEntryTitle = blogEntryTitle,
                blogEntryAuthor = blogEntryAuthor,
            )
        }.getOrNull()
    }

    private fun extractRecentBlogEntryOrNull(item: Element): CodeforcesRecentFeedBlogEntry? {
        return kotlin.runCatching {
            val author = item.expectRatedUser().extractRatedUser()
            val blogEntryId: Int
            val blogEntryTitle: String
            item.getElementsByAttributeValueStarting("href", "/blog/entry/")[0].let {
                blogEntryId = it.attr("href").removePrefix("/blog/entry/").toInt()
                blogEntryTitle = it.text()
            }
            CodeforcesRecentFeedBlogEntry(
                id = blogEntryId,
                title = blogEntryTitle,
                author = author,
                isLowRated = false
            )
        }.getOrNull()
    }


    internal fun extractBlogEntries(document: Document): List<CodeforcesWebBlogEntry> {
        return document.expectContent().select("div.topic")
            .mapNotNull(::extractBlogEntryOrNull)
    }

    fun extractBlogEntries(source: String): List<CodeforcesWebBlogEntry> =
        extractBlogEntries(Jsoup.parse(source))

    internal fun extractComments(document: Document): List<CodeforcesWebComment> {
        return document.expectContent().select(".comment-table")
            .mapNotNull(::extractCommentOrNull)
    }

    fun extractComments(source: String): List<CodeforcesWebComment> =
        extractComments(Jsoup.parse(source))

    internal fun extractRecentBlogEntries(document: Document): List<CodeforcesRecentFeedBlogEntry> {
        return document.expectSidebar().expectFirst("div.recent-actions")
            .select("li")
            .mapNotNull(::extractRecentBlogEntryOrNull)
    }

    fun extractRecentBlogEntries(source: String): List<CodeforcesRecentFeedBlogEntry> =
        extractRecentBlogEntries(Jsoup.parse(source))

    private inline fun extractContestPhaseInfo(source: String, block: (String, String) -> Unit) {
        val sidebar = Jsoup.parse(source).selectSidebar() ?: return
        val phaseText = sidebar.selectFirst("span.contest-state-phase")?.text()?.trim() ?: return
        val infoText = sidebar.selectFirst("span.contest-state-regular")?.text()?.trim() ?: return
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