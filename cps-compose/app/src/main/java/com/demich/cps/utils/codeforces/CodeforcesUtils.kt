package com.demich.cps.utils.codeforces

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import com.demich.cps.accounts.managers.HandleColor
import com.demich.cps.accounts.managers.NOT_RATED
import com.demich.cps.accounts.managers.STATUS
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.signedToString
import kotlinx.datetime.Instant
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.*

object CodeforcesUtils {

    private val dateFormatRU = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.US).apply { timeZone = TimeZone.getTimeZone("Europe/Moscow") }
    private val dateFormatEN = SimpleDateFormat("MMM/dd/yyyy HH:mm", Locale.US).apply { timeZone = TimeZone.getTimeZone("Europe/Moscow") }

    private fun String.extractTime(): Instant {
        val parser = if(this.contains('.')) dateFormatRU else dateFormatEN
        return Instant.fromEpochMilliseconds(parser.parse(this)!!.time)
    }

    private fun Element.extractRatedUser(): Pair<String, ColorTag> = Pair(
        first = text(),
        second = ColorTag.fromString(
            str = classNames().first { name -> name.startsWith("user-") }
        )
    )

    fun extractBlogEntries(source: String): List<CodeforcesBlogEntry> {
        return Jsoup.parse(source).select("div.topic").map { topic ->
            val id: Int
            val title: String
            topic.selectFirst("div.title")!!.let {
                title = it.selectFirst("p")!!.text()
                id = it.selectFirst("a")!!.attr("href").removePrefix("/blog/entry/").toInt()
            }

            val authorHandle: String
            val authorColorTag: ColorTag
            val creationTime: Instant
            topic.selectFirst("div.info")!!.let { info ->
                with(info.selectFirst(".rated-user")!!.extractRatedUser()) {
                    authorHandle = first
                    authorColorTag = second
                }
                creationTime = info.selectFirst(".format-humantime")!!.attr("title").extractTime()
            }

            val rating: Int
            val commentsCount: Int
            topic.selectFirst(".roundbox")!!.let { box ->
                rating = box.selectFirst(".left-meta")!!.selectFirst("span")!!.text().toInt()
                val commentsItem = box.selectFirst(".right-meta")!!.select("li")[2]!!
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
        }
    }

    fun extractComments(source: String): List<CodeforcesRecentAction> {
        return Jsoup.parse(source).select(".comment-table").map { commentBox ->
            val commentatorHandle: String
            val commentatorHandleColorTag: ColorTag
            commentBox.selectFirst(".avatar")!!.let { avatarBox ->
                with(avatarBox.selectFirst("a.rated-user")!!.extractRatedUser()) {
                    commentatorHandle = first
                    commentatorHandleColorTag = second
                }
            }

            val blogEntryId: Int
            val blogEntryTitle: String
            val blogEntryAuthorHandle: String
            val blogEntryAuthorHandleColorTag: ColorTag
            val commentId: Long
            val commentCreationTime: Instant
            val commentRating: Int
            commentBox.selectFirst("div.info")!!.let { info ->
                commentCreationTime = info.selectFirst(".format-humantime")!!.attr("title").extractTime()
                with(info.selectFirst("a.rated-user")!!.extractRatedUser()) {
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

            val commentHtml = commentBox.selectFirst("div.ttypography")?.html()
                ?: "" //<span class="notice">Комментарий удален по причине нарушения правил Codeforces</span>

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
        }
    }

    fun extractRecentBlogEntries(source: String): List<CodeforcesBlogEntry> {
        return Jsoup.parse(source).selectFirst("div.recent-actions")?.select("li")?.map { item ->
            val (handle, handleColorTag) = item.selectFirst("a.rated-user")!!.extractRatedUser()
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
        } ?: emptyList()
    }

    enum class ColorTag {
        BLACK,
        GRAY,
        GREEN,
        CYAN,
        BLUE,
        VIOLET,
        ORANGE,
        RED,
        LEGENDARY,
        ADMIN;

        companion object {
            fun fromString(str: String): ColorTag =
                valueOf(str.removePrefix("user-").uppercase(Locale.ENGLISH))
        }
    }

    fun getTagByRating(rating: Int): ColorTag {
        return when {
            rating == NOT_RATED -> ColorTag.BLACK
            rating < 1200 -> ColorTag.GRAY
            rating < 1400 -> ColorTag.GREEN
            rating < 1600 -> ColorTag.CYAN
            rating < 1900 -> ColorTag.BLUE
            rating < 2100 -> ColorTag.VIOLET
            rating < 2400 -> ColorTag.ORANGE
            rating < 3000 -> ColorTag.RED
            else -> ColorTag.LEGENDARY
        }
    }

    fun getHandleColorByTag(tag: ColorTag): HandleColor? {
        return when (tag) {
            ColorTag.GRAY -> HandleColor.GRAY
            ColorTag.GREEN -> HandleColor.GREEN
            ColorTag.CYAN -> HandleColor.CYAN
            ColorTag.BLUE -> HandleColor.BLUE
            ColorTag.VIOLET -> HandleColor.VIOLET
            ColorTag.ORANGE -> HandleColor.ORANGE
            ColorTag.RED, ColorTag.LEGENDARY -> HandleColor.RED
            else -> null
        }
    }

    suspend fun getRealHandle(handle: String): Pair<String, STATUS> {
        val page = CodeforcesApi.getPageSource(CodeforcesApi.urls.user(handle), CodeforcesLocale.EN) ?: return handle to STATUS.FAILED
        val realHandle = extractRealHandle(page) ?: return handle to STATUS.NOT_FOUND
        return realHandle to STATUS.OK
    }

    private fun extractRealHandle(page: String): String? {
        val userBox = Jsoup.parse(page).selectFirst("div.userbox") ?: return null
        return userBox.selectFirst("a.rated-user")?.text()
    }

    @Composable
    fun VoteRatingNonZero(rating: Int, fontSize: TextUnit, modifier: Modifier = Modifier) {
        if (rating != 0) {
            Text(
                text = signedToString(rating),
                color = if (rating > 0) cpsColors.votedRatingPositive else cpsColors.votedRatingNegative,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize,
                modifier = modifier
            )
        }
    }

    fun htmlToAnnotatedString(html: String) = buildAnnotatedString {
        Jsoup.parseBodyFragment(html).body().traverse(CodeforcesHtmlParser(this))
    }

}


enum class CodeforcesLocale {
    EN, RU;

    override fun toString(): String {
        return when(this){
            EN -> "en"
            RU -> "ru"
        }
    }
}