package com.demich.cps.platforms.utils.codeforces

import com.demich.cps.platforms.api.codeforces.CodeforcesBlogEntry
import com.demich.cps.platforms.api.codeforces.CodeforcesColorTag
import com.demich.cps.platforms.api.codeforces.CodeforcesComment
import org.jsoup.nodes.Element
import java.util.Locale

data class CodeforcesHandle(
    val handle: String,
    val colorTag: CodeforcesColorTag
)

val CodeforcesBlogEntry.author: CodeforcesHandle
    get() = CodeforcesHandle(
        handle = authorHandle,
        colorTag = authorColorTag
    )

val CodeforcesComment.commentator: CodeforcesHandle
    get() = CodeforcesHandle(
        handle = commentatorHandle,
        colorTag = commentatorHandleColorTag
    )


private fun Element.extractColorTag(): CodeforcesColorTag {
    val str = runCatching {
        classNames()
            .first { name -> name.startsWith("user-") }
            .removePrefix("user-")
            .uppercase(Locale.ENGLISH)
    }.getOrElse {
        return CodeforcesColorTag.BLACK
    }

    return kotlin.runCatching {
        enumValueOf<CodeforcesColorTag>(str)
    }.getOrElse {
        str.toIntOrNull()?.let { CodeforcesColorTag.fromRating(it) }
            ?: CodeforcesColorTag.BLACK
    }
}

internal fun Element.extractRatedUser(): CodeforcesHandle =
    CodeforcesHandle(
        handle = text(),
        colorTag = extractColorTag()
    )