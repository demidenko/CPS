package com.demich.cps.contests.loading

import com.demich.cps.contests.database.Contest
import com.demich.cps.platforms.api.niceMessage

enum class ContestsLoaderType(val supportedPlatforms: Set<Contest.Platform>) {
    clist_api(Contest.platforms.toSet()),
    codeforces_api(Contest.Platform.codeforces),
    atcoder_parse(Contest.Platform.atcoder),
    dmoj_api(Contest.Platform.dmoj)
    ;

    constructor(platform: Contest.Platform): this(setOf(platform))
}

fun makeCombinedMessage(
    errors: List<Pair<ContestsLoaderType, Throwable>>,
    exposeAll: Boolean
): String {
    if (errors.isEmpty()) return ""
    return errors.groupBy(
        keySelector = { (_, e) ->
            e.niceMessage ?: if (exposeAll) "$e" else "Some error..."
        },
        valueTransform = { it.first }
    ).entries.joinToString(separator = "; ") { (msg, list) -> "${list.distinct()}: $msg" }
}