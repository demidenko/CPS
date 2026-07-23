package com.demich.cps.platforms.utils.codeforces

import com.demich.cps.platforms.utils.EvaluatorTagWithClass
import com.demich.cps.platforms.utils.expectFirst
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Evaluator

internal interface CodeforcesPageParser {

}


context(parser: CodeforcesPageParser)
internal fun Document.expectContent(): Element = expectFirst("div.content-with-sidebar")

context(parser: CodeforcesPageParser)
internal fun Document.selectSidebar(): Element? = selectFirst("div#sidebar")

context(parser: CodeforcesPageParser)
internal fun Document.expectSidebar(): Element = requireNotNull(selectSidebar())


abstract class CodeforcesCommunityPageParser: CodeforcesPageParser {
    private val evaluatorDivInfo = EvaluatorTagWithClass(tag = "div", className = "info")
    protected fun Element.expectDivInfo(): Element = expectFirst(evaluatorDivInfo)

    private val evaluatorRatedUser = Evaluator.Class("rated-user")
    protected fun Element.selectRatedUser(): Element? = selectFirst(evaluatorRatedUser)
    protected fun Element.expectRatedUser(): Element = expectFirst(evaluatorRatedUser)

    private val evaluatorHumanTime = Evaluator.Class("format-humantime")
    protected fun Element.expectHumanTime(): Element = expectFirst(evaluatorHumanTime)

    private val evaluatorHrefBlogEntry = Evaluator.AttributeWithValueStarting("href", "/blog/entry/")
    protected fun Element.expectBlogEntryHref(): Element = expectFirst(evaluatorHrefBlogEntry)
}