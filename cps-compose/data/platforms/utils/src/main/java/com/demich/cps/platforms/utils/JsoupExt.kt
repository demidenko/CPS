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

// TODO: optimize without calling classNames()
internal fun Element.classNameStartsWithOrNull(prefix: String): String? {
    return classNames().firstOrNull { name -> name.startsWith(prefix) }
}