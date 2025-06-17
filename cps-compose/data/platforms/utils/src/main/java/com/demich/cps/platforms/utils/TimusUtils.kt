package com.demich.cps.platforms.utils

import com.demich.cps.accounts.userinfo.ProfileResult
import com.demich.cps.accounts.userinfo.TimusUserInfo
import com.demich.cps.accounts.userinfo.UserSuggestion
import org.jsoup.Jsoup

object TimusUtils {
    fun extractProfile(source: String, handle: String): ProfileResult<TimusUserInfo> {
        with(Jsoup.parse(source)) {
            val userName = selectFirst("h2.author_name")?.text()
                ?: return ProfileResult.NotFound(userId = handle)
            val rows =
                if (selectFirst("div.author_none_solved") != null)
                    listOf("0", "0", "0", "0")
                else select("td.author_stats_value").map { row ->
                    row.text().let { it.substring(0, it.indexOf(" out of ")) }
                }
            return ProfileResult.Success(
                userInfo = TimusUserInfo(
                    id = handle,
                    userName = userName,
                    rating = rows[3].toInt(),
                    solvedTasks = rows[1].toInt(),
                    rankTasks = rows[0].toInt(),
                    rankRating = rows[2].toInt()
                )
            )
        }
    }

    fun extractUsersSuggestions(source: String): List<UserSuggestion> =
        Jsoup.parse(source).expectFirst("table.ranklist")
            .select("td.name")
            .mapNotNull { nameColumn ->
                val userId = nameColumn.selectFirst("a")
                    ?.attr("href")
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