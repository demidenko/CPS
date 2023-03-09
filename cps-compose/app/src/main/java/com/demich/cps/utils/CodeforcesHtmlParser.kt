package com.demich.cps.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.select.NodeVisitor

class CodeforcesHtmlParser(
    private val builder: AnnotatedString.Builder,
    val quoteColor: Color
): NodeVisitor {
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
            //TODO rated span
            //makeHandleSpan requires Composable
            builder.append(text = e.text(), fontWeight = FontWeight.SemiBold)
            e.remove()
            return
        }

        if (name == "a") {
            builder.pushStyle(SpanStyle(textDecoration = TextDecoration.Underline))
            return
        }

        //TODO: bad
        if (name == "p" && builder.length > 0) {
            builder.append("\n\n")
            return
        }

        if (name == "b" || name == "strong") {
            builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            return
        }

        if (name == "i" || name == "em") {
            builder.pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
            return
        }

        if (name == "code") {
            builder.pushStyle(SpanStyle(fontFamily = FontFamily.Monospace))
            return
        }

        if (name == "blockquote") {
            builder.pushStyle(SpanStyle(fontStyle = FontStyle.Italic, color = quoteColor))
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