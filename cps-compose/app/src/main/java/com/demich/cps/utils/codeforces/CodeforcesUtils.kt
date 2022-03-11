package com.demich.cps.utils.codeforces

import com.demich.cps.accounts.HandleColor
import com.demich.cps.accounts.NOT_RATED
import com.demich.cps.accounts.STATUS
import com.demich.cps.news.CodeforcesLocale
import java.util.*

object CodeforcesUtils {

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
        val page = CodeforcesAPI.getPageSource(CodeforcesURLFactory.user(handle), CodeforcesLocale.EN) ?: return handle to STATUS.FAILED
        val realHandle = extractRealHandle(page) ?: return handle to STATUS.NOT_FOUND
        return realHandle to STATUS.OK
    }

    private fun extractRealHandle(s: String): String? {
        var i = s.indexOf(" <div class=\"userbox\">")
        if(i == -1) return null
        i = s.indexOf("<div class=\"user-rank\">", i)
        i = s.indexOf("class=\"rated-user", i)
        return s.substring(s.indexOf('>', i)+1, s.indexOf("</a", i))
    }

}


object CodeforcesURLFactory {

    const val main = "https://codeforces.com"

    fun user(handle: String) = "$main/profile/$handle"

    fun blog(blogId: Int) = "$main/blog/entry/$blogId"

    fun comment(blogId: Int, commentId: Long) = blog(blogId) + "#comment-$commentId"

    fun contest(contestId: Int) = "$main/contest/$contestId"

    fun contestOuter(contestId: Int) = "$main/contests/$contestId"

    fun contestsWith(handle: String) = "$main/contests/with/$handle"

    //fun submission(submission: CodeforcesSubmission) = "$main/contest/${submission.contestId}/submission/${submission.id}"

    fun problem(contestId: Int, problemIndex: String) = "$main/contest/$contestId/problem/$problemIndex"
}