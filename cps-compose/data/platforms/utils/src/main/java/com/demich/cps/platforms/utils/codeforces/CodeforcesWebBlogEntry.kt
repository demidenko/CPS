package com.demich.cps.platforms.utils.codeforces

import com.demich.cps.platforms.api.codeforces.models.CodeforcesBlogEntry
import com.demich.cps.platforms.api.codeforces.models.CodeforcesColorTag
import kotlinx.serialization.Serializable
import org.jsoup.Jsoup
import kotlin.time.Instant

@Serializable
data class CodeforcesWebBlogEntry(
    val id: Int,
    val title: String,
    val author: CodeforcesHandle,
    val creationTime: Instant,
    val rating: Int,
    val commentsCount: Int
)

fun CodeforcesBlogEntry.extractTitle(): String =
    Jsoup.parse(title).text()

fun CodeforcesBlogEntry.toWebBlogEntry(colorTag: CodeforcesColorTag) =
    CodeforcesWebBlogEntry(
        id = id,
        title = title,
        author = CodeforcesHandle(handle = authorHandle, colorTag = colorTag),
        creationTime = creationTime,
        rating = rating,
        commentsCount = 0
    )