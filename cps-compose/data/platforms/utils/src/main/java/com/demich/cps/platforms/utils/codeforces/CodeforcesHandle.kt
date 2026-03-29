package com.demich.cps.platforms.utils.codeforces

import kotlinx.serialization.Serializable
import org.jsoup.nodes.Element
import java.util.Locale

@Serializable
data class CodeforcesHandle(
    val handle: String,
    val colorTag: CodeforcesColorTag
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