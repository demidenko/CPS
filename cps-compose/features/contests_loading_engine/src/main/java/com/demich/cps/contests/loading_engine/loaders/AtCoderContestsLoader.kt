package com.demich.cps.contests.loading_engine.loaders

import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.loading.ContestsLoaderType
import com.demich.cps.platforms.utils.AtCoderUtils
import com.demich.cps.platforms.api.AtCoderApi
import com.demich.kotlin_stdlib_boost.splitTrailingBrackets

class AtCoderContestsLoader: ContestsLoader(type = ContestsLoaderType.atcoder_parse) {
    override suspend fun loadContests(platform: Contest.Platform): List<Contest> =
        AtCoderUtils.extractContests(source = AtCoderApi.getContestsPage())
}


internal fun fixAtCoderTitle(origin: String): String {
    val s = origin
        .replace("（", " (")
        .replace("）",") ")
        .trimEnd()

    s.splitTrailingBrackets { title, brackets ->
        if (brackets.isNotEmpty()) {
            val t = brackets.removeSurrounding("(", ")")
            if (t.isContestTitle()) return "$t (${title.trim()})"
        }
    }

    return s
}

private fun String.isContestTitle(): Boolean {
    return matches(Regex("^atcoder (beginner|regular|grand|heuristic) contest .*", RegexOption.IGNORE_CASE))
}