package com.demich.cps.ui.topbar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.demich.cps.ui.CPSDefaults
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.randomUuid
import kotlin.math.max
import kotlin.math.min

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
        Column(modifier = modifier) {
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
internal fun SubTitle(
    text: () -> String
) {
    val letters: List<Pair<Char, Long>> by remember(text) {
        var prev = ""
        var ids = longArrayOf()
        derivedStateOf {
            val cur = text()
            val prefix = longestCommonPrefix(cur, prev)
            val newIds = LongArray(cur.length) { if (it < prefix) ids[it] else randomUuid() }
            subsetIndices(
                a = prev.substring(startIndex = prefix),
                b = cur.substring(startIndex = prefix)
            ) { i, j ->
                newIds[prefix + j] = ids[prefix + i]
            }
            cur.toList().zip(newIds.asList()).also {
                prev = cur
                ids = newIds
            }
        }
    }

    LazyRow(modifier = Modifier.fillMaxWidth()) {
        items(
            items = letters,
            key = { it }
        ) {
            Text(
                text = "${it.first}",
                modifier = Modifier.animateItem()
            )
        }
    }
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
    val gb = b.indices.groupBy { b[it] }
    ga.forEach { (char, va) ->
        val vb = gb.getOrDefault(char, emptyList())
        //TODO: use random for biggest list
        repeat(times = min(va.size, vb.size)) {
            block(va[it], vb[it])
        }
    }
}

private fun longestCommonPrefix(a: String, b: String): Int {
    var i = 0
    while (i < a.length && i < b.length && a[i] == b[i]) ++i
    return i
}