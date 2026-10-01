package com.demich.cps.platforms.utils.codeforces

import com.demich.cps.platforms.utils.classNameFirstOrNull
import org.jsoup.nodes.Element
import java.util.Locale

data class CodeforcesHandle(
    val handle: String,
    val userTag: CodeforcesUserTag
)

private fun Element.extractUserTag(): CodeforcesUserTag? {
    val str = classNameFirstOrNull { name -> name.startsWith("user-") }
        ?.removePrefix("user-")
        ?.uppercase(Locale.ENGLISH)
        ?: return null

    return try {
        CodeforcesHandleCssTag.valueOf(str).toUserTag()
    } catch (_: IllegalArgumentException) {
        // user-4000 case
        str.toIntOrNull()?.let {
            CodeforcesRatingColorTag.fromRating(it)
        }
    }
}

internal fun Element.extractRatedUser(): CodeforcesHandle =
    CodeforcesHandle(
        handle = text(),
        userTag = extractUserTag() ?: CodeforcesUnratedTag
    )