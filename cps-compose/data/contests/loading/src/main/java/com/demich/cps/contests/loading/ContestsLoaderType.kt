package com.demich.cps.contests.loading

import com.demich.cps.contests.database.Contest

enum class ContestsLoaderType(val supportedPlatforms: Set<Contest.Platform>) {
    clist_api(Contest.platforms.toSet()),
    codeforces_api(Contest.Platform.codeforces),
    atcoder_parse(Contest.Platform.atcoder),
    dmoj_api(Contest.Platform.dmoj)
    ;

    constructor(platform: Contest.Platform): this(setOf(platform))
}