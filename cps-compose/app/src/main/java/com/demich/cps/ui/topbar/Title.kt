package com.demich.cps.ui.topbar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.ui.CPSDefaults
import com.demich.cps.ui.theme.cpsColors
import com.demich.kotlin_stdlib_boost.commonPrefixLengthWith
import com.demich.kotlin_stdlib_boost.takeRandom
import kotlin.uuid.Uuid

@Composable
internal fun Title(
    subtitle: () -> String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 15.sp
) {
    ProvideTextStyle(
        value = CPSDefaults.MonospaceTextStyle.copy(
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
            color = cpsColors.contentAdditional
        )
    ) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "Competitive Programming && Solving",
                color = cpsColors.content,
                maxLines = 1
            )
            SubTitle(text = subtitle)
        }
    }
}

@Composable
private fun SubTitle(
    modifier: Modifier = Modifier,
    text: () -> String
) {
    val titleChars by rememberTitleCharsState(text)

    LazyRow(modifier = modifier) {
        items(
            items = titleChars,
            key = { it }
        ) {
            Text(
                text = "${it.first}",
                modifier = Modifier.animateItem()
            )
        }
    }
}

@Composable
private fun rememberTitleCharsState(
    text: () -> String
): State<List<Pair<Char, Uuid>>> =
    produceState(
        initialValue = TitleChars("", emptyList()),
        key1 = text
    ) {
        snapshotFlow { text() }
            .collect { cur ->
                val (prev, prevIds) = value
                val prefix = cur.commonPrefixLengthWith(prev)
                val ids = MutableList(cur.length) { if (it < prefix) prevIds[it] else Uuid.random() }
                subsetIndices(
                    a = prev.substring(startIndex = prefix),
                    b = cur.substring(startIndex = prefix)
                ) { i, j ->
                    ids[prefix + j] = prevIds[prefix + i]
                }
                value = TitleChars(title = cur, ids = ids)
            }
    }

private data class TitleChars(
    val title: String,
    val ids: List<Uuid>
): AbstractList<Pair<Char, Uuid>>() {

    init {
        require(title.length == ids.size)
    }

    override val size: Int get() = title.length

    override fun get(index: Int) = Pair(title[index], ids[index])
}

private inline fun subsetIndices(a: String, b: String, block: (Int, Int) -> Unit) {
    a.distinctFlat().forEach { char ->
        val va = a.indicesOf(char)
        val vb = b.indicesOf(char)
//        val n = min(va.size, vb.size)
//        val sa = va.takeRandom(n)
//        val sb = vb.takeRandom(n)
//        zip(a = sa, b = sb, block = block)
        if (va.size < vb.size) {
            zip(a = va, b = vb.takeRandom(va.size), block = block)
        } else {
            zip(a = va.takeRandom(vb.size), b = vb, block = block)
        }
    }
}

private fun String.indicesOf(char: Char) =
    indices.filter { get(it) == char }

private fun String.distinctFlat(): Iterable<Char> {
//    return toSet()
    val set = mutableListOf<Char>()
    forEach { if (it !in set) set.add(it) }
    return set
}

private inline fun zip(
    a: List<Int>,
    b: List<Int>,
    block: (Int, Int) -> Unit
) {
    require(a.size == b.size)
    repeat(a.size) { block(a[it], b[it]) }
}
