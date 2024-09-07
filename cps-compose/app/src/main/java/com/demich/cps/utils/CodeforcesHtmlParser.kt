package com.demich.cps.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.demich.cps.LocalCodeforcesAccountManager
import com.demich.cps.accounts.managers.CodeforcesAccountManager
import com.demich.cps.platforms.api.codeforces.CodeforcesColorTag
import com.demich.cps.platforms.utils.codeforces.CodeforcesHtmlParser
import com.demich.cps.platforms.utils.codeforces.CodeforcesHtmlStringBuilder
import com.demich.cps.platforms.utils.codeforces.parseCodeforcesHtml
import com.demich.cps.ui.theme.CPSColors
import com.demich.cps.ui.theme.cpsColors

@Composable
@ReadOnlyComposable
fun htmlToAnnotatedString(html: String): AnnotatedString {
    val builder = AnnotatedString.Builder()
    parseCodeforcesHtml(
        html = html,
        parser = CodeforcesHtmlParser(
            builder = CodeforcesHtmlStringBuilderImpl(
                builder = builder,
                cpsColors = cpsColors,
                manager = LocalCodeforcesAccountManager.current
            )
        )
    )
    return builder.toAnnotatedString()
}


private class CodeforcesHtmlStringBuilderImpl(
    val builder: AnnotatedString.Builder,
    val cpsColors: CPSColors,
    val manager: CodeforcesAccountManager
): CodeforcesHtmlStringBuilder {
    val quoteColor get() = cpsColors.contentAdditional

    override val length get() = builder.length

    override fun append(text: String) = builder.append(text)

    override fun appendRatedSpan(handle: String, tag: CodeforcesColorTag) {
        builder.append(manager.makeHandleSpan(handle = handle, tag = tag, cpsColors = cpsColors))
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

    override fun pushStroke() {
        builder.pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
    }
}


