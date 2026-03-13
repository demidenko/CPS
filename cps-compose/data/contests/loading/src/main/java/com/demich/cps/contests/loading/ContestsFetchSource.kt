package com.demich.cps.contests.loading

import com.demich.cps.contests.database.Contest
import com.demich.kotlin_stdlib_boost.toEnumSet

enum class ContestsFetchSource(val platforms: Set<Contest.Platform>) {
    clist_api(platforms = Contest.platforms.toEnumSet()),
    codeforces_api(platform = codeforces),
    atcoder_parse(platform = atcoder),
    dmoj_api(platform = dmoj)
    ;

    constructor(platform: Contest.Platform): this(setOf(platform))
}