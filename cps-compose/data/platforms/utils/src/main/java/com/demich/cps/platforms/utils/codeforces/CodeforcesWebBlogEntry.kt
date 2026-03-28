package com.demich.cps.platforms.utils.codeforces

import com.demich.cps.platforms.api.codeforces.models.CodeforcesBlogEntry
import org.jsoup.Jsoup
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
    Jsoup.parse(title).text()