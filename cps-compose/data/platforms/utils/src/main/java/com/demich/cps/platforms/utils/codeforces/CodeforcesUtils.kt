package com.demich.cps.platforms.utils.codeforces

import com.demich.cps.platforms.api.codeforces.CodeforcesPageContentProvider
import com.demich.cps.platforms.api.codeforces.models.CodeforcesBlogEntry
import com.demich.cps.platforms.api.codeforces.models.CodeforcesColorTag
import com.demich.cps.platforms.api.codeforces.models.CodeforcesComment
import com.demich.cps.platforms.api.codeforces.models.CodeforcesProblem
import com.demich.cps.platforms.api.codeforces.models.CodeforcesRecentAction
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.alternativeParsing
import kotlinx.datetime.format.char
import kotlinx.datetime.toDeprecatedInstant
import kotlinx.datetime.toInstant
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

private fun Element.expectContent(): Element = expectFirst("div.content-with-sidebar")

private fun Element.selectSidebar(): Element? = selectFirst("div#sidebar")
private fun Element.expectSidebar(): Element = requireNotNull(selectSidebar())

private fun Element.expectDivInfo(): Element = expectFirst("div.info")

private fun Element.selectRatedUser(): Element? = selectFirst("a.rated-user")
private fun Element.expectRatedUser(): Element = requireNotNull(selectRatedUser())

private fun Element.expectHumanTimeTitle(): String = expectFirst(".format-humantime").attr("title")

object CodeforcesUtils {
    private object DateTimeParser {
        private val moscowTimeZone = kotlinx.datetime.TimeZone.of("Europe/Moscow")

        private val dateTimeFormat = LocalDateTime.Format {
            alternativeParsing({
                //RU format: "dd.MM.yyyy HH:mm"
                dayOfMonth()
                char('.')
                monthNumber()
                char('.')
                year()
            }) {
                //EN format: "MMM/dd/yyyy HH:mm"
                monthName(MonthNames.ENGLISH_ABBREVIATED)
                char('/')
                dayOfMonth()
                char('/')
                year()
            }
            char(' ')
            hour()
            char(':')
            minute()
        }

        fun parse(input: String): Instant =
            LocalDateTime.parse(input, dateTimeFormat).toInstant(moscowTimeZone).toDeprecatedInstant()
    }

    private fun String.extractTime(): Instant = DateTimeParser.parse(this)

    private fun extractBlogEntryOrNull(topic: Element): CodeforcesBlogEntry? {
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

            CodeforcesBlogEntry(
                id = id,
                title = title,
                authorHandle = author.handle,
                authorColorTag = author.colorTag,
                creationTime = creationTime,
                rating = rating,
                commentsCount = commentsCount
            )
        }.getOrNull()
    }

    private fun extractCommentOrNull(commentBox: Element): CodeforcesRecentAction? {
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

            CodeforcesRecentAction(
                time = commentCreationTime,
                comment = CodeforcesComment(
                    id = commentId,
                    commentatorHandle = commentator.handle,
                    commentatorHandleColorTag = commentator.colorTag,
                    html = commentHtml,
                    rating = commentRating,
                    creationTime = commentCreationTime
                ),
                blogEntry = CodeforcesBlogEntry(
                    id = blogEntryId,
                    title = blogEntryTitle,
                    authorHandle = blogEntryAuthor.handle,
                    authorColorTag = blogEntryAuthor.colorTag,
                    creationTime = Instant.DISTANT_PAST,
                    rating = 0
                )
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

    fun extractTitle(blogEntry: CodeforcesBlogEntry): String =
        Jsoup.parse(blogEntry.title).text()

    fun extractBlogEntries(source: String): List<CodeforcesBlogEntry> {
        return Jsoup.parse(source).expectContent().select("div.topic")
            .mapNotNull(::extractBlogEntryOrNull)
    }

    fun extractComments(source: String): List<CodeforcesRecentAction> {
        return Jsoup.parse(source).expectContent().select(".comment-table")
            .mapNotNull(::extractCommentOrNull)
    }

    fun extractRecentBlogEntries(source: String): List<CodeforcesRecentFeedBlogEntry> {
        return Jsoup.parse(source).expectSidebar().expectFirst("div.recent-actions")
            .select("li")
            .mapNotNull(::extractRecentBlogEntryOrNull)
    }

    private inline fun extractProblemWithAcceptedCount(
        problemRow: Element,
        contestId: Int,
        block: (CodeforcesProblem, Int) -> Unit
    ) {
        val td = problemRow.select("td")
        if (td.isEmpty()) return
        val acceptedCount = td[3].text().trim().run {
            if (!startsWith('x')) return
            substring(1).toInt()
        }
        val problem = CodeforcesProblem(
            index = td[0].text().trim(),
            name = td[1].expectFirst("a").text(),
            contestId = contestId
        )
        block(problem, acceptedCount)
    }

    fun extractContestAcceptedStatistics(source: String, contestId: Int): Map<CodeforcesProblem, Int> {
        return buildMap {
            Jsoup.parse(source).expectFirst("table.problems").select("tr")
                .forEach {
                    extractProblemWithAcceptedCount(it, contestId, ::put)
                }
        }
    }

    fun extractContestSystemTestingPercentageOrNull(source: String): Int? {
        return Jsoup.parse(source).selectSidebar()?.selectFirst("span.contest-state-regular")
            ?.text()
            ?.removeSuffix("%")
            ?.toIntOrNull()
    }

    fun colorTagFrom(rating: Int?): CodeforcesColorTag =
        when {
            rating == null -> CodeforcesColorTag.BLACK
            rating < 1200 -> CodeforcesColorTag.GRAY
            rating < 1400 -> CodeforcesColorTag.GREEN
            rating < 1600 -> CodeforcesColorTag.CYAN
            rating < 1900 -> CodeforcesColorTag.BLUE
            rating < 2100 -> CodeforcesColorTag.VIOLET
            rating < 2400 -> CodeforcesColorTag.ORANGE
            rating < 3000 -> CodeforcesColorTag.RED
            else -> CodeforcesColorTag.LEGENDARY
        }
}


suspend fun CodeforcesPageContentProvider.getRealColorTagOrNull(handle: String): CodeforcesColorTag? {
    val page = runCatching { getUserPage(handle) }.getOrElse { return null }
    return Jsoup.parse(page).selectFirst("div.userbox")
        ?.selectRatedUser()
        ?.extractRatedUser()
        ?.colorTag
}

suspend inline fun CodeforcesPageContentProvider.getHandleSuggestions(
    str: String,
    block: (String) -> Unit
) {
    getHandleSuggestionsPage(str).splitToSequence('\n').filter { !it.contains('=') }.forEach {
        val i = it.indexOf('|')
        if (i != -1) block(it.substring(i + 1))
    }
}