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


internal interface CodeforcesRatedUserSelector: CodeforcesPageParser {
    fun Element.selectRatedUser(): Element?
}

context(selector: CodeforcesRatedUserSelector)
internal fun Element.expectRatedUser(): Element = with(selector) { requireNotNull(selectRatedUser()) }

internal class CodeforcesRatedUserSelectorImpl: CodeforcesRatedUserSelector {
    private val evaluatorRatedUser = Evaluator.Class("rated-user")
    override fun Element.selectRatedUser(): Element? = selectFirst(evaluatorRatedUser)
}

internal interface CodeforcesDivInfoSelector: CodeforcesPageParser {
    fun Element.expectDivInfo(): Element
}

internal class CodeforcesDivInfoSelectorImpl: CodeforcesDivInfoSelector {
    private val evaluatorDivInfo = EvaluatorTagWithClass(tag = "div", className = "info")
    override fun Element.expectDivInfo(): Element = expectFirst(evaluatorDivInfo)
}

internal interface CodeforcesHumanTimeSelector: CodeforcesPageParser {
    fun Element.expectHumanTime(): Element
}

internal class CodeforcesHumanTimeSelectorImpl: CodeforcesHumanTimeSelector {
    private val evaluatorHumanTime = Evaluator.Class("format-humantime")
    override fun Element.expectHumanTime(): Element = expectFirst(evaluatorHumanTime)
}

abstract class CodeforcesCommunityPageParser: CodeforcesRatedUserSelector by CodeforcesRatedUserSelectorImpl() {
    private val evaluatorHrefBlogEntry = Evaluator.AttributeWithValueStarting("href", blogEntryHrefPrefix)
    protected fun Element.expectBlogEntryHref(): Element = expectFirst(evaluatorHrefBlogEntry)
}

const val blogEntryHrefPrefix = "/blog/entry/"

internal fun String.extractBlogEntryIdFromBlogEntryHref(): Int {
    // href="/blog/entry/XXXXXX"
    // href="/blog/entry/XXXXXX#comment-YYYYYY"
    require(startsWith(blogEntryHrefPrefix)) {
        "href \"$this\" is not starts with \"$blogEntryHrefPrefix\""
    }
    val i = blogEntryHrefPrefix.length
    val j = indexOf('#', startIndex = i).let {
        if (it == -1) length else it
    }
    return substring(startIndex = i, endIndex = j).toInt()
}