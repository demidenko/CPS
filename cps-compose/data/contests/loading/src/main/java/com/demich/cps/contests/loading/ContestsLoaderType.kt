package com.demich.cps.contests.loading

import com.demich.cps.contests.database.Contest
import com.demich.kotlin_stdlib_boost.toEnumSet

enum class ContestsLoaderType(val supportedPlatforms: Set<Contest.Platform>) {
    clist_api(Contest.platforms.toEnumSet()),
    codeforces_api(Contest.Platform.codeforces),
    atcoder_parse(Contest.Platform.atcoder),
    dmoj_api(Contest.Platform.dmoj)
    ;

    constructor(platform: Contest.Platform): this(setOf(platform))
}