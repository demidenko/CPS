package com.demich.cps.platforms.utils.codeforces

import com.demich.cps.platforms.api.CodeforcesBlogEntry
import com.demich.cps.platforms.api.CodeforcesRecentAction

data class CodeforcesRecent(
    val blogEntries: List<CodeforcesBlogEntry>,
    val comments: List<CodeforcesRecentAction>
)