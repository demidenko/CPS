package com.demich.cps.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.demich.cps.platforms.api.CodeforcesColorTag
import com.demich.cps.platforms.utils.codeforces.CodeforcesHtmlParser
import com.demich.cps.platforms.utils.codeforces.CodeforcesHtmlStringBuilder
import com.demich.cps.platforms.utils.codeforces.parseCodeforcesHtml
import com.demich.cps.ui.theme.CPSColors
import com.demich.cps.ui.theme.cpsColors

@Composable
fun htmlToAnnotatedString(html: String): AnnotatedString {
    val builder = AnnotatedString.Builder()
    parseCodeforcesHtml(
        html = html,
        parser = CodeforcesHtmlParser(
            builder = CodeforcesHtmlStringBuilderImpl(
                builder = builder,
                cpsColors = cpsColors
            )
        )
    )
    return builder.toAnnotatedString()
}


private class CodeforcesHtmlStringBuilderImpl(
    val builder: AnnotatedString.Builder,
    val cpsColors: CPSColors
): CodeforcesHtmlStringBuilder {
    val quoteColor get() = cpsColors.contentAdditional

    override val length get() = builder.length

    override fun append(text: String) = builder.append(text)

    override fun appendRatedSpan(handle: String, tag: CodeforcesColorTag) {
        //TODO rated span
        //makeHandleSpan requires Composable
        builder.append(text = handle, fontWeight = FontWeight.SemiBold)
    }

    override fun pop() = builder.pop()

    override fun pushLink() {
        builder.pushStyle(SpanStyle(textDecoration = TextDecoration.Underline))
    }

    override fun pushBold() {
        builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
    }

    override fun pushItalic() {
        builder.pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
    }

    override fun pushMono() {
        builder.pushStyle(SpanStyle(fontFamily = FontFamily.Monospace))
    }

    override fun pushQuote() {
        builder.pushStyle(SpanStyle(fontStyle = FontStyle.Italic, color = quoteColor))
    }
}


