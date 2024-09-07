package com.demich.cps.platforms.utils.codeforces

import com.demich.cps.platforms.api.codeforces.CodeforcesBlogEntry
import com.demich.cps.platforms.api.codeforces.CodeforcesRecentAction

data class CodeforcesRecent(
    val blogEntries: List<CodeforcesBlogEntry>,
    val comments: List<CodeforcesRecentAction>
)