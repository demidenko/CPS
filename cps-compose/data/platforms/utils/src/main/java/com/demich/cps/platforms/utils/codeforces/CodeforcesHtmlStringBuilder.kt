package com.demich.cps.platforms.utils.codeforces

import com.demich.cps.platforms.api.CodeforcesColorTag
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.select.NodeVisitor


interface CodeforcesHtmlStringBuilder {
    val length: Int
    fun append(text: String)
    fun appendRatedSpan(handle: String, tag: CodeforcesColorTag)
    fun pop()
    fun pushLink()
    fun pushBold()
    fun pushItalic()
    fun pushMono()
    fun pushQuote()
}

fun parseCodeforcesHtml(html: String, parser: CodeforcesHtmlParser) {
    Jsoup.parseBodyFragment(html).body().traverse(parser)
}

class CodeforcesHtmlParser(val builder: CodeforcesHtmlStringBuilder): NodeVisitor {
    override fun head(node: Node, depth: Int) {
        //println("+${node.nodeName()}")
        if (node is TextNode) {
            builder.append(node.text())
            return
        }

        val e = node as? Element ?: return
        val name = e.normalName()
        if (name == "body") return

        /*
        TODO:
        <li>: ul - circles; ol - 1, 2, 3...
         */

        if (name == "a" && e.hasClass("rated-user")) {
            val (handle, tag) = e.extractRatedUser()
            builder.appendRatedSpan(handle = handle, tag = tag)
            e.remove()
            return
        }

        if (name == "a") {
            builder.pushLink()
            return
        }

        if (name == "br") {
            builder.append("\n")
            return
        }

        //TODO: bad
        if (name == "p" && builder.length > 0) {
            builder.append("\n\n")
            return
        }

        if (name == "b" || name == "strong") {
            builder.pushBold()
            return
        }

        if (name == "i" || name == "em") {
            builder.pushItalic()
            return
        }

        if (name == "code") {
            builder.pushMono()
            return
        }

        if (name == "blockquote") {
            builder.pushQuote()
            return
        }

        if (name == "img") {
            builder.append("[pic]")
            return
        }
    }

    override fun tail(node: Node, depth: Int) {
        val e = node as? Element ?: return

        when (e.normalName()) {
            "a" -> {
                builder.pop()
            }
            "b", "strong" -> builder.pop()
            "i", "em" -> builder.pop()
            "code" -> builder.pop()
            "blockquote" -> builder.pop()
        }
    }

}