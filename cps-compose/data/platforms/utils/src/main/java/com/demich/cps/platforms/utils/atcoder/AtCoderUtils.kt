package com.demich.cps.platforms.utils.atcoder

import com.demich.cps.accounts.userinfo.AtCoderUserInfo
import com.demich.cps.accounts.userinfo.UserSuggestion
import com.demich.cps.platforms.utils.NewsPostEntry
import kotlinx.datetime.Instant
import org.jsoup.Jsoup

object AtCoderUtils {
    fun extractUserInfo(source: String): AtCoderUserInfo =
        with(Jsoup.parse(source)) {
            AtCoderUserInfo(
                handle = expectFirst("a.username").text(),
                rating = select("th.no-break").find { it.text() == "Rating" }
                    ?.nextElementSibling()
                    ?.selectFirst("span")
                    ?.text()?.toInt()
            )
        }

    fun extractUserSuggestions(source: String): List<UserSuggestion> {
        val table = Jsoup.parse(source).expectFirst("table.table")
        val ratingIndex = table.select("thead > tr > th").indexOfFirst { it.text() == "Rating" }
        return table.select("tbody > tr").map { row ->
            UserSuggestion(
                userId = row.expectFirst("a.username").text(),
                info = row.select("td")[ratingIndex].text()
            )
        }
    }

    class NewsPost(
        val title: String,
        val time: Instant?,
        override val id: String
    ): NewsPostEntry


    fun extractNews(source: String): List<NewsPost?> =
        Jsoup.parse(source).select("div.panel.panel-default, div.panel.panel-info")
            .mapNotNull { panel ->
                val header = panel.expectFirst("div.panel-heading")
                val titleElement = header.expectFirst("h3.panel-title")
                val timeElement = header.selectFirst("span.tooltip-unix") ?: return@mapNotNull null
                val id = titleElement.expectFirst("a").attr("href").removePrefix("/posts/")
                NewsPost(
                    title = titleElement.text(),
                    time = timeElement.let {
                        Instant.fromEpochSeconds(it.attr("title").toLong())
                    },
                    id = id
                )
            }
            .sortedByDescending { it.id }
}