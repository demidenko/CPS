package com.demich.cps.contests.loading_engine.loaders

import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.loading.ContestsFetchSource
import com.demich.cps.platforms.api.atcoder.AtCoderApi
import com.demich.cps.platforms.utils.atcoder.getContests
import com.demich.kotlin_stdlib_boost.splitTrailingBrackets

class AtCoderContestsFetcher(val api: AtCoderApi): ContestsFetcher() {
    override val fetchSource get() = ContestsFetchSource.atcoder_parse

    override suspend fun getContests(platform: Contest.Platform): List<Contest> =
        api.getContests()
}


internal fun correctAtCoderTitle(origin: String): String {
    val s = origin
        .replace("（", " (")
        .replace("）",") ")
        .trimEnd()

    s.splitTrailingBrackets { str, brackets ->
        if (brackets.isNotEmpty()) {
            val title = brackets.substring(1, brackets.length - 1)
            if (title.isContestTitle()) return "$title ${brackets.first()}${str.trim()}${brackets.last()}"
        }
    }

    return s
}

private fun String.isContestTitle(): Boolean {
    return matches(Regex("^atcoder [a-z]+ contest .*", RegexOption.IGNORE_CASE))
}