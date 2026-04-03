package com.demich.cps.platforms.utils

import org.jsoup.nodes.Element
import org.jsoup.select.Evaluator
import kotlin.streams.asSequence

internal fun Element.expectFirst(evaluator: Evaluator): Element =
    requireNotNull(selectFirst(evaluator))

internal fun Element.selectSequence(cssQuery: String): Sequence<Element> =
    selectStream(cssQuery).asSequence()

internal fun Element.selectSequence(evaluator: Evaluator): Sequence<Element> =
    selectStream(evaluator).asSequence()

internal inline fun Element.classNameFirstOrNull(predicate: (String) -> Boolean): String? {
    // return classNames().firstOrNull(predicate)
    // classNames() is terribly inefficient so do manually
    val s = className()
    var i = 0
    while (i < s.length) {
        while (i < s.length && s[i].isWhitespace()) ++i
        val p = i
        while (i < s.length && !s[i].isWhitespace()) ++i
        val name = s.substring(p, i)
        if (predicate(name)) return name
    }
    return null
}

internal class EvaluatorTagWithClass(val tag: String, val className: String): Evaluator() {
    override fun matches(root: Element, element: Element): Boolean {
        return element.normalName() == tag && element.hasClass(className)
    }
}