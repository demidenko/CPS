package com.demich.cps.platforms.utils.codeforces

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

internal interface CodeforcesPageParser {

}


context(parser: CodeforcesPageParser)
internal fun Document.expectContent(): Element = expectFirst("div.content-with-sidebar")

context(parser: CodeforcesPageParser)
internal fun Document.selectSidebar(): Element? = selectFirst("div#sidebar")

context(parser: CodeforcesPageParser)
internal fun Document.expectSidebar(): Element = requireNotNull(selectSidebar())
