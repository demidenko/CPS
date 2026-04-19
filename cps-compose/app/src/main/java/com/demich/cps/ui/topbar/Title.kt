package com.demich.cps.ui.topbar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.ui.CPSDefaults
import com.demich.cps.ui.theme.cpsColors
import com.demich.kotlin_stdlib_boost.takeRandom
import kotlin.math.max
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
    val titleState = remember { mutableStateOf(TitleChars("", emptyList())) }

    LaunchedEffect(text) {
        snapshotFlow { text() }
            .collect { cur ->
                val (prev, prevIds) = titleState.value
                val prefix = longestCommonPrefix(cur, prev)
                val ids = Array(cur.length) { if (it < prefix) prevIds[it] else Uuid.random() }
                subsetIndices(
                    a = prev.substring(startIndex = prefix),
                    b = cur.substring(startIndex = prefix)
                ) { i, j ->
                    ids[prefix + j] = prevIds[prefix + i]
                }
                titleState.value = TitleChars(title = cur, ids = ids.asList())
            }
    }

    LazyRow(modifier = modifier) {
        items(
            items = titleState.value,
            key = { it }
        ) {
            Text(
                text = "${it.first}",
                modifier = Modifier.animateItem()
            )
        }
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


// longest common subsequence
private inline fun lcsIndices(a: String, b: String, block: (Int, Int) -> Unit) {
    val d = Array(a.length + 1) { IntArray(b.length + 1) }
    for (i in 0 .. a.length)
    for (j in 0 .. b.length) {
        d[i][j] = when {
            i == 0 || j == 0 -> 0
            a[i-1] == b[j-1] -> d[i-1][j-1] + 1
            else -> max(d[i-1][j], d[i][j-1])
        }
    }
    var i = a.length
    var j = b.length
    while (i > 0 && j > 0) {
        when {
            d[i][j] == d[i-1][j-1]+1 && a[i-1] == b[j-1] -> {
                block(i-1, j-1)
                --i
                --j
            }
            d[i][j] == d[i-1][j] -> --i
            d[i][j] == d[i][j-1] -> --j
        }
    }
}

private inline fun subsetIndices(a: String, b: String, block: (Int, Int) -> Unit) {
    //TODO: speedup by not using maps
    val ga = a.indices.groupBy { a[it] }
//    val gb = b.indices.groupBy { b[it] }
    ga.forEach { (char, va) ->
//        val vb = gb.getOrElse(char) { return@forEach }
        val vb = b.indices.filter { b[it] == char }
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

private inline fun zip(
    a: List<Int>,
    b: List<Int>,
    block: (Int, Int) -> Unit
) {
    require(a.size == b.size)
    repeat(a.size) { block(a[it], b[it]) }
}

private fun longestCommonPrefix(a: String, b: String): Int {
    var i = 0
    while (i < a.length && i < b.length && a[i] == b[i]) ++i
    return i
}