package com.demich.cps.platforms.utils.codeforces

import kotlin.time.Instant

data class CodeforcesWebBlogEntry(
    val id: Int,
    val title: String,
    val author: CodeforcesHandle,
    val creationTime: Instant,
    val rating: Int,
    val commentsCount: Int
)