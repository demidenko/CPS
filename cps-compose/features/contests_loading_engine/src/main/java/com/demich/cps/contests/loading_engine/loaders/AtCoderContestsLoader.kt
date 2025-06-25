package com.demich.cps.contests.loading_engine.loaders

import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.loading.ContestsLoaderType
import com.demich.cps.platforms.clients.AtCoderClient
import com.demich.cps.platforms.utils.AtCoderUtils
import com.demich.kotlin_stdlib_boost.splitTrailingBrackets

class AtCoderContestsLoader: ContestsLoader() {
    override val type get() = ContestsLoaderType.atcoder_parse

    override suspend fun loadContests(platform: Contest.Platform): List<Contest> =
        AtCoderUtils.extractContests(source = AtCoderClient.getContestsPage())
}


internal fun correctAtCoderTitle(origin: String): String {
    val s = origin
        .replace("（", " (")
        .replace("）",") ")
        .trimEnd()

    s.splitTrailingBrackets { title, brackets ->
        if (brackets.isNotEmpty()) {
            val t = brackets.substring(1, brackets.length - 1)
            if (t.isContestTitle()) return "$t ${brackets.first()}${title.trim()}${brackets.last()}"
        }
    }

    return s
}

private fun String.isContestTitle(): Boolean {
    return matches(Regex("^atcoder (beginner|regular|grand|heuristic) contest .*", RegexOption.IGNORE_CASE))
}