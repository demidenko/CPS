package com.demich.cps.platforms.utils.codeforces

import kotlin.time.Instant

data class CodeforcesWebComment(
    val id: Long,
    val author: CodeforcesHandle,
    val html: String,
    val rating: Int,
    val creationTime: Instant,
    val blogEntryId: Int,
    val blogEntryTitle: String,
    val blogEntryAuthor: CodeforcesHandle
)