package com.demich.cps.contests.loading

import com.demich.cps.contests.database.Contest
import java.util.EnumSet

enum class ContestsLoaderType(val supportedPlatforms: Set<Contest.Platform>) {
    clist_api(EnumSet.copyOf(Contest.platforms)),
    codeforces_api(Contest.Platform.codeforces),
    atcoder_parse(Contest.Platform.atcoder),
    dmoj_api(Contest.Platform.dmoj)
    ;

    constructor(platform: Contest.Platform): this(setOf(platform))
}