package com.demich.cps.platforms.utils.codeforces

import com.demich.cps.platforms.api.codeforces.models.CodeforcesBlogEntry
import com.demich.cps.platforms.utils.parseHtmlElement
import kotlin.time.Instant

data class CodeforcesWebBlogEntry(
    val id: Int,
    val title: String,
    val author: CodeforcesHandle,
    val creationTime: Instant,
    val rating: Int,
    val commentsCount: Int
)

fun CodeforcesBlogEntry.extractTitle(): String =
    title.parseHtmlElement().text()

fun CodeforcesBlogEntry.toWebBlogEntry(colorTag: CodeforcesColorTag) =
    CodeforcesWebBlogEntry(
        id = id,
        title = extractTitle(),
        author = CodeforcesHandle(handle = authorHandle, colorTag = colorTag),
        creationTime = creationTime,
        rating = rating,
        commentsCount = 0
    )