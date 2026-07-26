package com.demich.cps.platforms.utils

import com.demich.cps.profiles.userinfo.TimusUserInfo
import com.demich.cps.profiles.userinfo.UserSuggestion

class TimusParser {
    fun extractUserInfo(source: String, handle: String): TimusUserInfo? {
        with(source.parseDocument()) {
            val userName = selectFirst("h2.author_name")?.text()
                ?: return null
            val rows =
                if (selectFirst("div.author_none_solved") != null)
                    listOf("0", "0", "0", "0")
                else select("td.author_stats_value").map { row ->
                    row.text().let { it.substring(0, it.indexOf(" out of ")) }
                }
            return TimusUserInfo(
                id = handle,
                userName = userName,
                rating = rows[3].toInt(),
                solvedTasks = rows[1].toInt(),
                rankTasks = rows[0].toInt(),
                rankRating = rows[2].toInt()
            )
        }
    }

    fun extractUsersSuggestions(source: String): List<UserSuggestion> =
        source.parseDocument().expectFirst("table.ranklist")
            .select("td.name")
            .mapNotNull { nameColumn ->
                val userId = nameColumn.selectFirst("a")
                    ?.href
                    ?.let {
                        it.substring(it.indexOf("id=")+3)
                    } ?: return@mapNotNull null
                val tasks = nameColumn.nextElementSibling()?.nextElementSibling()?.text() ?: ""
                UserSuggestion(
                    userId = userId,
                    title = nameColumn.text(),
                    info = tasks
                )
            }
}