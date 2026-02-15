package com.demich.cps.platforms.utils.codeforces

import com.demich.cps.platforms.api.codeforces.models.CodeforcesBlogEntry
import com.demich.cps.platforms.api.codeforces.models.CodeforcesColorTag
import com.demich.cps.platforms.api.codeforces.models.CodeforcesComment
import kotlinx.serialization.Serializable
import org.jsoup.nodes.Element
import java.util.Locale

@Serializable
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


private fun Element.extractColorTag(): CodeforcesColorTag? {
    val str = classNames()
        .firstOrNull { name -> name.startsWith("user-") }
        ?.removePrefix("user-")
        ?.uppercase(Locale.ENGLISH)
        ?: return null

    return kotlin.runCatching {
        enumValueOf<CodeforcesColorTag>(str)
    }.getOrElse {
        str.toIntOrNull()?.let {
            CodeforcesUtils.colorTagFrom(it)
        }
    }
}

internal fun Element.extractRatedUser(): CodeforcesHandle =
    CodeforcesHandle(
        handle = text(),
        colorTag = extractColorTag() ?: BLACK
    )