package com.demich.cps.platforms.utils.codeforces

import com.demich.cps.platforms.api.codeforces.CodeforcesColorTag
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
    fun pushStroke()
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

        if (name.isLink() && e.hasClass("rated-user")) {
            val user = e.extractRatedUser()
            builder.appendRatedSpan(handle = user.handle, tag = user.colorTag)
            e.remove()
            return
        }

        if (name.isLink()) {
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

        if (name.isBold()) {
            builder.pushBold()
            return
        }

        if (name.isItalic()) {
            builder.pushItalic()
            return
        }

        if (name.isCode()) {
            builder.pushMono()
            return
        }

        if (name.isQuote()) {
            builder.pushQuote()
            return
        }

        if (name.isStroke()) {
            builder.pushStroke()
            return
        }

        if (name == "img") {
            builder.append("[pic]")
            return
        }
    }

    override fun tail(node: Node, depth: Int) {
        val e = node as? Element ?: return
        val name = e.normalName()


        if (name.isLink() || name.isBold() || name.isItalic() || name.isCode() || name.isQuote() || name.isStroke()) {
            builder.pop()
            return
        }
    }

}

private fun String.isLink() = this == "a"
private fun String.isBold() = this == "b" || this == "strong"
private fun String.isItalic() = this == "i" || this == "em"
private fun String.isCode() = this == "code"
private fun String.isQuote() = this == "blockquote"
private fun String.isStroke() = this == "s"
