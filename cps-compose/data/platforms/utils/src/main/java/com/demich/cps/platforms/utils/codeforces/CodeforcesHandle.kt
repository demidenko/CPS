package com.demich.cps.platforms.utils.codeforces

import com.demich.cps.platforms.api.CodeforcesBlogEntry
import com.demich.cps.platforms.api.CodeforcesColorTag
import com.demich.cps.platforms.api.CodeforcesComment

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