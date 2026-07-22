package com.demich.cps.platforms.utils.atcoder

import com.demich.cps.platforms.utils.NewsPostEntry
import com.demich.cps.platforms.utils.href
import com.demich.cps.platforms.utils.parseDocument
import com.demich.cps.profiles.userinfo.AtCoderUserInfo
import com.demich.cps.profiles.userinfo.UserSuggestion
import org.jsoup.nodes.Element
import kotlin.time.Instant

class AtCoderParser {
    fun extractUserInfo(source: String): AtCoderUserInfo =
        with(source.parseDocument()) {
            AtCoderUserInfo(
                handle = expectFirst("a.username").text(),
                rating = select("th.no-break").find { it.text() == "Rating" }
                    ?.nextElementSibling()
                    ?.selectFirst("span")
                    ?.text()?.toInt()
            )
        }

    fun extractUserSuggestions(source: String): List<UserSuggestion> {
        val table = source.parseDocument().expectFirst("table.table")
        val ratingIndex = table.select("thead > tr > th").indexOfFirst { it.text() == "Rating" }
        return table.select("tbody > tr").map { row ->
            UserSuggestion(
                userId = row.expectFirst("a.username").text(),
                info = row.select("td")[ratingIndex].text()
            )
        }
    }

    fun extractNews(source: String): List<AtcoderNewsPost?> =
        source.parseDocument()
            .select("div.panel.panel-default, div.panel.panel-info")
            .mapNotNull { it.extractNewsFromPanel() }
            .sortedByDescending { it.time }

    private fun Element.extractNewsFromPanel(): AtcoderNewsPost? {
        val header = selectFirst("div.panel-heading") ?: return null
        val titleElement = header.expectFirst("h3.panel-title")
        val timeElement = header.selectFirst("span.tooltip-unix") ?: return null
        val id = titleElement.expectFirst("a").href.removePrefix("/posts/")
        return AtcoderNewsPost(
            title = titleElement.text(),
            time = Instant.fromEpochSeconds(timeElement.attr("title").toLong()),
            id = id
        )
    }
}

data class AtcoderNewsPost(
    val title: String,
    val time: Instant,
    override val id: String
): NewsPostEntry