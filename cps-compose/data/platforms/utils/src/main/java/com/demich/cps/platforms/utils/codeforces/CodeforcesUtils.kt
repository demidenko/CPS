package com.demich.cps.platforms.utils.codeforces

import com.demich.cps.accounts.userinfo.CodeforcesUserInfo
import com.demich.cps.accounts.userinfo.STATUS
import com.demich.cps.platforms.api.CodeforcesAPIErrorResponse
import com.demich.cps.platforms.api.CodeforcesApi
import com.demich.cps.platforms.api.CodeforcesBlogEntry
import com.demich.cps.platforms.api.CodeforcesColorTag
import com.demich.cps.platforms.api.CodeforcesComment
import com.demich.cps.platforms.api.CodeforcesProblem
import com.demich.cps.platforms.api.CodeforcesRecentAction
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.alternativeParsing
import kotlinx.datetime.format.char
import kotlinx.datetime.toInstant
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import kotlin.math.max


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
            LocalDateTime.parse(input, dateTimeFormat).toInstant(moscowTimeZone)
    }

    private fun String.extractTime(): Instant = DateTimeParser.parse(this)

    private fun extractBlogEntryOrNull(topic: Element): CodeforcesBlogEntry? {
        return kotlin.runCatching {
            val id: Int
            val title: String
            topic.expectFirst("div.title").let {
                title = it.expectFirst("p").text()
                id = it.expectFirst("a").attr("href").removePrefix("/blog/entry/").toInt()
            }

            val author: CodeforcesHandle
            val creationTime: Instant
            topic.expectFirst("div.info").let { info ->
                author = info.expectFirst(".rated-user").extractRatedUser()
                creationTime = info.expectFirst(".format-humantime").attr("title").extractTime()
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
                .expectFirst("a.rated-user")
                .extractRatedUser()

            val blogEntryId: Int
            val blogEntryTitle: String
            val blogEntryAuthor: CodeforcesHandle
            val commentId: Long
            val commentCreationTime: Instant
            val commentRating: Int
            commentBox.expectFirst("div.info").let { info ->
                commentCreationTime = info.expectFirst(".format-humantime").attr("title").extractTime()
                blogEntryAuthor = info.expectFirst("a.rated-user").extractRatedUser()
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
                    creationTime = Instant.DISTANT_PAST
                )
            )
        }.getOrNull()
    }

    private fun extractRecentBlogEntryOrNull(item: Element): CodeforcesBlogEntry? {
        return kotlin.runCatching {
            val author = item.expectFirst("a.rated-user").extractRatedUser()
            val blogEntryId: Int
            val blogEntryTitle: String
            item.getElementsByAttributeValueStarting("href", "/blog/entry/")[0].let {
                blogEntryId = it.attr("href").removePrefix("/blog/entry/").toInt()
                blogEntryTitle = it.text()
            }
            CodeforcesBlogEntry(
                id = blogEntryId,
                title = blogEntryTitle,
                authorHandle = author.handle,
                authorColorTag = author.colorTag,
                creationTime = Instant.DISTANT_PAST
            )
        }.getOrNull()
    }

    private fun Element.expectContent(): Element = expectFirst("div.content-with-sidebar")
    private fun Element.expectSidebar(): Element = expectFirst("div#sidebar")

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

    fun extractRecentBlogEntries(source: String): List<CodeforcesBlogEntry> {
        return Jsoup.parse(source).expectSidebar().expectFirst("div.recent-actions")
            .select("li")
            .mapNotNull(::extractRecentBlogEntryOrNull)
    }

    fun extractRecentActions(source: String): CodeforcesRecent {
        val comments = extractComments(source)
        //blog entry with low rating disappeared from blogEntries but has comments, need to merge
        val blogEntries = extractRecentBlogEntries(source).toMutableList()
        val blogEntriesIds = blogEntries.mapTo(mutableSetOf()) { it.id }
        val usedIds = mutableSetOf<Int>()
        var index = 0
        for (comment in comments) {
            val blogEntry = comment.blogEntry!!
            val id = blogEntry.id
            if (id !in blogEntriesIds) {
                blogEntriesIds.add(id)
                if (index < blogEntries.size) {
                    //mark low rated
                    blogEntries.add(
                        index = index,
                        element = blogEntry.copy(rating = -1)
                    )
                } else {
                    //latest recent comments has no blog entries in recent action, so most likely not low rated
                    require(index == blogEntries.size)
                    blogEntries.add(blogEntry)
                }
            }
            if (id !in usedIds) {
                usedIds.add(id)
                val curIndex = blogEntries.indexOfFirst { it.id == id }
                index = max(index, curIndex + 1)
            }
        }
        return CodeforcesRecent(blogEntries, comments)
    }

    inline fun extractHandleSuggestions(source: String, block: (String) -> Unit) {
        source.splitToSequence('\n').filter { !it.contains('=') }.forEach {
            val i = it.indexOf('|')
            if (i != -1) block(it.substring(i + 1))
        }
    }

    private suspend fun getUserPageOrNull(handle: String): String? =
        CodeforcesApi.runCatching { getUserPage(handle) }.getOrNull()

    private suspend fun getRealHandle(handle: String): Pair<String, STATUS> {
        val page = getUserPageOrNull(handle) ?: return handle to STATUS.FAILED
        val realHandle = extractRealHandle(page)?.handle ?: return handle to STATUS.NOT_FOUND
        return realHandle to STATUS.OK
    }

    suspend fun getRealColorTag(handle: String): CodeforcesColorTag {
        return getUserPageOrNull(handle)?.let { extractRealHandle(it)?.colorTag } ?: CodeforcesColorTag.BLACK
    }

    private fun extractRealHandle(page: String): CodeforcesHandle? {
        val userBox = Jsoup.parse(page).selectFirst("div.userbox") ?: return null
        return userBox.selectFirst("a.rated-user")?.extractRatedUser()
    }


    suspend fun getUsersInfo(handles: Collection<String>, doRedirect: Boolean) =
        getUsersInfo(handles.toSet(), doRedirect)

    //TODO: non recursive O(n) version
    suspend fun getUsersInfo(handles: Set<String>, doRedirect: Boolean): Map<String, CodeforcesUserInfo> {
        return CodeforcesApi.runCatching {
            getUsers(handles = handles, checkHistoricHandles = doRedirect)
                .apply { require(size == handles.size) }
        }.map { infos ->
            //relying to cf api return in same order
            handles.zip(infos.map { CodeforcesUserInfo(it) }).toMap()
        }.getOrElse { e ->
            if (e is CodeforcesAPIErrorResponse) {
                e.isHandleNotFound()?.let { badHandle ->
                    return@getOrElse getUsersInfo(handles = handles - badHandle, doRedirect = doRedirect)
                        .plus(badHandle to CodeforcesUserInfo(handle = badHandle, status = STATUS.NOT_FOUND))
                }
            }
            handles.associateWith { handle -> CodeforcesUserInfo(handle = handle, status = STATUS.FAILED) }
        }.apply {
            require(handles.all { it in this })
        }
    }

    suspend fun getUserInfo(handle: String, doRedirect: Boolean): CodeforcesUserInfo {
        //return getUsersInfo(setOf(handle), doRedirect).getValue(handle)
        return CodeforcesApi.runCatching {
            CodeforcesUserInfo(getUser(handle = handle, checkHistoricHandles = doRedirect))
        }.getOrElse { e ->
            if (e is CodeforcesAPIErrorResponse && e.isHandleNotFound() == handle) {
                CodeforcesUserInfo(handle = handle, status = STATUS.NOT_FOUND)
            } else {
                CodeforcesUserInfo(handle = handle, status = STATUS.FAILED)
            }
        }
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
        return Jsoup.parse(source).expectSidebar().selectFirst("span.contest-state-regular")
            ?.text()
            ?.removeSuffix("%")
            ?.toIntOrNull()
    }
}