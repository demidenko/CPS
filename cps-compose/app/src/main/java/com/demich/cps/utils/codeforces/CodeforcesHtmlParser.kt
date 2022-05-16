package com.demich.cps.utils.codeforces

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.select.NodeVisitor

class CodeforcesHtmlParser(private val builder: AnnotatedString.Builder): NodeVisitor {
    override fun head(node: Node, depth: Int) {
        //println("+${node.nodeName()}")
        if (node is TextNode) {
            builder.append(node.text())
            return
        }
        val e = node as? Element ?: return
        if (e.normalName() == "body") return
        if (e.normalName() == "a" && e.hasClass("rated-user")) {
            //TODO rated span

            return
        }
        if (e.normalName() == "p" && builder.length > 0) {
            builder.append("\n\n")
            return
        }

        if (e.normalName() == "b" || e.normalName() == "strong") {
            builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            return
        }

        if (e.normalName() == "i" || e.normalName() == "em") {
            builder.pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
            return
        }

        if (e.normalName() == "code") {
            builder.pushStyle(SpanStyle(fontFamily = FontFamily.Monospace))
            return
        }
    }

    override fun tail(node: Node, depth: Int) {
        val e = node as? Element ?: return
        if (e.normalName() == "b" || e.normalName() == "strong") {
            builder.pop()
            return
        }

        if (e.normalName() == "i" || e.normalName() == "em") {
            builder.pop()
            return
        }

        if (e.normalName() == "code") {
            builder.pop()
            return
        }
    }

}