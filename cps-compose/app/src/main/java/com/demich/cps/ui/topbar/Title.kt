package com.demich.cps.ui.topbar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.demich.cps.ui.CPSDefaults
import com.demich.cps.ui.theme.cpsColors

@Composable
internal fun Title(
    subtitle: () -> String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 15.sp
) {
    ProvideTextStyle(
        value = CPSDefaults.MonospaceTextStyle.copy(
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold
        )
    ) {
        Column(modifier = modifier) {
            Text(
                text = "Competitive Programming && Solving",
                color = cpsColors.content,
                maxLines = 1
            )
            SubTitle(
                text = subtitle,
                color = cpsColors.contentAdditional
            )
        }
    }
}

@Composable
internal fun SubTitle(
    text: () -> String,
    color: Color
) {
    //TODO: improve animation by longest common subseq

    val prefixes by remember(text) {
        derivedStateOf {
            text().runningFold("") { pref, it -> pref + it }.drop(1)
        }
    }

    LazyRow {
        items(
            items = prefixes,
            key = { it }
        ) {
            Text(text = "${it.last()}", color = color, modifier = Modifier.animateItem())
        }
    }
}