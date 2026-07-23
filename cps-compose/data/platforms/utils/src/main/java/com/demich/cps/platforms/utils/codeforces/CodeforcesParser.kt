package com.demich.cps.platforms.utils.codeforces

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

internal interface CodeforcesHtmlParser {

}


context(parser: CodeforcesHtmlParser)
internal fun Document.expectContent(): Element = expectFirst("div.content-with-sidebar")

context(parser: CodeforcesHtmlParser)
internal fun Document.selectSidebar(): Element? = selectFirst("div#sidebar")

context(parser: CodeforcesHtmlParser)
internal fun Document.expectSidebar(): Element = requireNotNull(selectSidebar())
