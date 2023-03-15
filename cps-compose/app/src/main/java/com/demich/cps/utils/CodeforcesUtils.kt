package com.demich.cps.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.buildAnnotatedString
import com.demich.cps.accounts.userinfo.CodeforcesUserInfo
import com.demich.cps.accounts.userinfo.STATUS
import com.demich.cps.data.api.*
import com.demich.cps.ui.theme.cpsColors
import kotlinx.datetime.Instant
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.*

object CodeforcesUtils {

    private val dateFormatRU = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.US).apply { timeZone = TimeZone.getTimeZone("Europe/Moscow") }
    private val dateFormatEN = SimpleDateFormat("MMM/dd/yyyy HH:mm", Locale.US).apply { timeZone = TimeZone.getTimeZone("Europe/Moscow") }

    private fun String.extractTime(): Instant {
        val parser = if (this.contains('.')) dateFormatRU else dateFormatEN
        return Instant.fromEpochMilliseconds(parser.parse(this)!!.time)
    }

    private fun Element.extractRatedUser(): Pair<String, CodeforcesColorTag> = Pair(
        first = text(),
        second = CodeforcesColorTag.fromString(
            str = classNames().first { name -> name.startsWith("user-") }
        )
    )

    private fun extractBlogEntryOrNull(topic: Element): CodeforcesBlogEntry? {
        return kotlin.runCatching {
            val id: Int
            val title: String
            topic.expectFirst("div.title").let {
                title = it.expectFirst("p").text()
                id = it.expectFirst("a").attr("href").removePrefix("/blog/entry/").toInt()
            }

            val authorHandle: String
            val authorColorTag: CodeforcesColorTag
            val creationTime: Instant
            topic.expectFirst("div.info").let { info ->
                with(info.expectFirst(".rated-user").extractRatedUser()) {
                    authorHandle = first
                    authorColorTag = second
                }
                creationTime = info.expectFirst(".format-humantime").attr("title").extractTime()
            }

            val rating: Int
            val commentsCount: Int
            topic.expectFirst(".roundbox").let { box ->
                rating = box.expectFirst(".left-meta").expectFirst("span").text().toInt()
                val commentsItem = box.expectFirst(".right-meta").select("li")[2]!!
                commentsCount = commentsItem.select("a")[1]!!.text().toInt()
            }

            CodeforcesBlogEntry(
                id = id,
                title = title,
                authorHandle = authorHandle,
                authorColorTag = authorColorTag,
                creationTime = creationTime,
                rating = rating,
                commentsCount = commentsCount
            )
        }.getOrNull()
    }

    private fun extractCommentOrNull(commentBox: Element): CodeforcesRecentAction? {
        return kotlin.runCatching {
            val commentatorHandle: String
            val commentatorHandleColorTag: CodeforcesColorTag
            commentBox.expectFirst(".avatar").let { avatarBox ->
                with(avatarBox.expectFirst("a.rated-user").extractRatedUser()) {
                    commentatorHandle = first
                    commentatorHandleColorTag = second
                }
            }

            val blogEntryId: Int
            val blogEntryTitle: String
            val blogEntryAuthorHandle: String
            val blogEntryAuthorHandleColorTag: CodeforcesColorTag
            val commentId: Long
            val commentCreationTime: Instant
            val commentRating: Int
            commentBox.expectFirst("div.info").let { info ->
                commentCreationTime = info.expectFirst(".format-humantime").attr("title").extractTime()
                with(info.expectFirst("a.rated-user").extractRatedUser()) {
                    blogEntryAuthorHandle = first
                    blogEntryAuthorHandleColorTag = second
                }
                info.getElementsByAttributeValueContaining("href", "#comment")[0]!!.let { commentLink ->
                    with(commentLink.attr("href").split("#comment-")) {
                        blogEntryId = this[0].removePrefix("/blog/entry/").toInt()
                        commentId = this[1].toLong()
                    }
                    blogEntryTitle = commentLink.text()
                }
                info.getElementsByAttribute("commentid")[0]!!.let { ratingBox ->
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
                    commentatorHandle = commentatorHandle,
                    commentatorHandleColorTag = commentatorHandleColorTag,
                    html = commentHtml,
                    rating = commentRating,
                    creationTime = commentCreationTime
                ),
                blogEntry = CodeforcesBlogEntry(
                    id = blogEntryId,
                    title = blogEntryTitle,
                    authorHandle = blogEntryAuthorHandle,
                    authorColorTag = blogEntryAuthorHandleColorTag,
                    creationTime = Instant.DISTANT_PAST
                )
            )
        }.getOrNull()
    }

    private fun extractRecentBlogEntryOrNull(item: Element): CodeforcesBlogEntry? {
        return kotlin.runCatching {
            val (handle, handleColorTag) = item.expectFirst("a.rated-user").extractRatedUser()
            val blogEntryId: Int
            val blogEntryTitle: String
            item.getElementsByAttributeValueStarting("href", "/blog/entry/")[0]!!.let {
                blogEntryId = it.attr("href").removePrefix("/blog/entry/").toInt()
                blogEntryTitle = it.text()
            }
            CodeforcesBlogEntry(
                id = blogEntryId,
                title = blogEntryTitle,
                authorHandle = handle,
                authorColorTag = handleColorTag,
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

    inline fun extractHandleSuggestions(source: String, block: (String) -> Unit) {
        source.splitToSequence('\n').filter { !it.contains('=') }.forEach {
            val i = it.indexOf('|')
            if (i != -1) block(it.substring(i + 1))
        }
    }

    suspend fun getRealHandle(handle: String): Pair<String, STATUS> {
        val page = CodeforcesApi.getUserPage(handle) ?: return handle to STATUS.FAILED
        val realHandle = extractRealHandle(page)?.first ?: return handle to STATUS.NOT_FOUND
        return realHandle to STATUS.OK
    }

    suspend fun getRealColorTag(handle: String): CodeforcesColorTag {
        return CodeforcesApi.getUserPage(handle)?.let { extractRealHandle(it)?.second } ?: CodeforcesColorTag.BLACK
    }

    private fun extractRealHandle(page: String): Pair<String, CodeforcesColorTag>? {
        val userBox = Jsoup.parse(page).selectFirst("div.userbox") ?: return null
        return userBox.selectFirst("a.rated-user")?.extractRatedUser()
    }


    suspend fun getUsersInfo(handles: Collection<String>, doRedirect: Boolean) =
        getUsersInfo(handles.toSet(), doRedirect)

    suspend fun getUsersInfo(handles: Set<String>, doRedirect: Boolean): Map<String, CodeforcesUserInfo> {
        return CodeforcesApi.runCatching {
            getUsers(handles = handles)
        }.map { infos ->
            handles.associateWith { handle ->
                infos.find { handle.equals(it.handle, ignoreCase = true) }
                    ?.let { CodeforcesUserInfo(it) }
                    ?: CodeforcesUserInfo(handle = handle, status = STATUS.FAILED)
            }
        }.getOrElse { e ->
            if (e is CodeforcesAPIErrorResponse) {
                e.isHandleNotFound()?.let { badHandle ->
                    val (realHandle, status) =
                        if (doRedirect) getRealHandle(handle = badHandle)
                        else badHandle to STATUS.NOT_FOUND
                    return@getOrElse if (status == STATUS.OK) {
                        val withReplaced = getUsersInfo(handles = handles - badHandle + realHandle, doRedirect = doRedirect)
                        handles.associateWith { handle ->
                            if (handle == badHandle) withReplaced.getValue(realHandle)
                            else withReplaced.getValue(handle)
                        }
                    } else {
                        getUsersInfo(handles = handles - badHandle, doRedirect = doRedirect)
                            .plus(badHandle to CodeforcesUserInfo(handle = badHandle, status = status))
                    }
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
            CodeforcesUserInfo(getUser(handle = handle))
        }.getOrElse { e ->
            if (e is CodeforcesAPIErrorResponse && e.isHandleNotFound() == handle) {
                if (doRedirect) {
                    val (realHandle, status) = getRealHandle(handle = handle)
                    if (status == STATUS.OK) getUserInfo(handle = realHandle, doRedirect = false)
                    else CodeforcesUserInfo(handle = handle, status = status)
                } else {
                    CodeforcesUserInfo(handle = handle, status = STATUS.NOT_FOUND)
                }
            } else {
                CodeforcesUserInfo(handle = handle, status = STATUS.FAILED)
            }
        }
    }

    private fun extractProblemWithAccepteds(problemRow: Element, contestId: Int): Pair<CodeforcesProblem, Int>? {
        return kotlin.runCatching {
            val td = problemRow.select("td")
            val acceptedCount = td[3].text().trim().removePrefix("x").toInt()
            val problem = CodeforcesProblem(
                index = td[0].text().trim(),
                name = td[1].expectFirst("a").text(),
                contestId = contestId
            )
            problem to acceptedCount
        }.getOrNull()
    }

    suspend fun getContestAcceptedStatistics(contestId: Int): Map<CodeforcesProblem, Int>? {
        val src = CodeforcesApi.getContestPage(contestId) ?: return null
        return Jsoup.parse(src).selectFirst("table.problems")
            ?.select("tr")
            ?.mapNotNull { extractProblemWithAccepteds(it, contestId) }
            ?.toMap()
    }

    suspend fun getContestSystemTestingPercentage(contestId: Int): Int? {
        val src = CodeforcesApi.getContestPage(contestId) ?: return null
        return Jsoup.parse(src).selectFirst("span.contest-state-regular")
            ?.text()
            ?.removeSuffix("%")
            ?.toIntOrNull()
    }

    @Composable
    fun htmlToAnnotatedString(html: String) = buildAnnotatedString {
        val parser = CodeforcesHtmlParser(
            builder = this,
            quoteColor = cpsColors.contentAdditional
        )
        Jsoup.parseBodyFragment(html).body().traverse(parser)
    }

}