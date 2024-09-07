package com.demich.cps.platforms.utils.codeforces

import com.demich.cps.platforms.api.codeforces.models.CodeforcesBlogEntry
import com.demich.cps.platforms.api.codeforces.models.CodeforcesRecentAction

data class CodeforcesRecent(
    val blogEntries: List<CodeforcesBlogEntry>,
    val comments: List<CodeforcesRecentAction>
)