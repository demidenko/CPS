package com.demich.cps.platforms.utils.codeforces

import com.demich.cps.platforms.api.codeforces.CodeforcesPageContentProvider
import com.demich.cps.platforms.utils.parseDocument
import com.demich.cps.platforms.utils.parseHtmlElement
import org.jsoup.nodes.Element
import org.jsoup.select.Evaluator

private val evaluatorRatedUser = Evaluator.Class("rated-user")
private fun Element.selectRatedUser(): Element? = selectFirst(evaluatorRatedUser)


suspend fun CodeforcesPageContentProvider.getRealColorTagOrNull(handle: String): CodeforcesColorTag? {
    val page = runCatching { getUserPage(handle) }.getOrElse { return null }
    return page.parseDocument()
        .selectFirst("div.userbox")
        ?.selectRatedUser()
        ?.extractRatedUser()
        ?.colorTag
}

suspend fun CodeforcesPageContentProvider.getHandleSuggestions(str: String): Sequence<CodeforcesHandle> =
    getHandleSuggestionsPage(str)
        .splitToSequence('\n')
        .filter { it.isNotEmpty() }
        .mapNotNull {
            val i = it.lastIndexOf('|')
            it.substring(i + 1).parseHtmlElement()
                .selectRatedUser()
                ?.extractRatedUser()
        }

