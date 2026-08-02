package com.demich.cps.platforms.utils.atcoder

import com.demich.cps.platforms.utils.EvaluatorTagWithClass
import com.demich.cps.platforms.utils.EvaluatorTagWithId
import com.demich.cps.platforms.utils.NewsPostEntry
import com.demich.cps.platforms.utils.expectFirst
import com.demich.cps.platforms.utils.href
import com.demich.cps.platforms.utils.parseDocument
import com.demich.cps.platforms.utils.selectSequence
import com.demich.cps.profiles.userinfo.AtCoderUserInfo
import com.demich.cps.profiles.userinfo.UserSuggestion
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Evaluator
import kotlin.time.Instant

class AtCoderParser {
    private fun Document.expectUserMainContainer() =
        expectFirst(EvaluatorTagWithId(tag = "div", id = "main-container"))

    fun extractUserInfo(source: String): AtCoderUserInfo {
        val container = source.parseDocument().expectUserMainContainer()
        val handle = container.expectFirst(Evaluator.Class("username")).text()
        val rating = container.selectSequence(EvaluatorTagWithClass(tag = "th", className = "no-break"))
            .find { it.text() == "Rating" }
            ?.nextElementSibling()
            ?.selectFirst("span")
            ?.text()?.toInt()

        return AtCoderUserInfo(
            handle = handle,
            rating = rating
        )
    }

    fun extractUserSuggestions(source: String): List<UserSuggestion> {
        val table = source.parseDocument().expectFirst("table.table")
        val ratingIndex = table.select("thead > tr > th").indexOfFirst { it.text() == "Rating" }
        return table.selectSequence("tbody > tr").map { row ->
            UserSuggestion(
                userId = row.expectFirst("a.username").text(),
                info = row.select("td")[ratingIndex].text()
            )
        }.toList()
    }

    fun extractNews(source: String): List<AtcoderNewsPost?> =
        source.parseDocument()
            .selectSequence("div.panel.panel-default, div.panel.panel-info")
            .mapNotNull { it.extractNewsFromPanel() }
            .toList()
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