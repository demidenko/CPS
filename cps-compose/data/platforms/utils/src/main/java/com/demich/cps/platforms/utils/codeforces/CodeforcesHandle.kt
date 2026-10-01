package com.demich.cps.platforms.utils.codeforces

import com.demich.cps.platforms.utils.classNameFirstOrNull
import org.jsoup.nodes.Element
import java.util.Locale

data class CodeforcesHandle(
    val handle: String,
    val colorTag: CodeforcesColorTag
)

val CodeforcesHandle.userTag: CodeforcesUserTag
    get() = colorTag.toUserTag()

private fun Element.extractColorTag(): CodeforcesColorTag? {
    val str = classNameFirstOrNull { name -> name.startsWith("user-") }
        ?.removePrefix("user-")
        ?.uppercase(Locale.ENGLISH)
        ?: return null

    return try {
        CodeforcesColorTag.valueOf(str)
    } catch (_: IllegalArgumentException) {
        // user-4000 case
        str.toIntOrNull()?.let {
            CodeforcesRatingColorTag.fromRating(it).toColorTag()
        }
    }
}

internal fun Element.extractRatedUser(): CodeforcesHandle =
    CodeforcesHandle(
        handle = text(),
        colorTag = extractColorTag() ?: BLACK
    )