package com.demich.cps.ui.settings

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow
import com.demich.cps.ui.CPSFontSize
import com.demich.cps.ui.WordsWithCounterOnOverflow
import com.demich.cps.ui.theme.cpsColors


@Composable
fun SettingsSubtitle(
    text: String
) {
    Text(
        text = text,
        fontSize = CPSFontSize.settingsSubtitle,
        color = cpsColors.contentAdditional,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun SettingsSubtitleOfEnabled(
    enabled: Collection<String>,
    allSize: Int? = null
) {
    when (enabled.size) {
        0 -> SettingsSubtitle("none selected")
        allSize -> SettingsSubtitle("all selected")
        else -> WordsWithCounterOnOverflow(words = enabled)
    }
}

@Composable
fun<T: Enum<T>> SettingsSubtitleOfEnabled(
    enabled: Set<T>,
    allSize: Int? = null,
    name: (T) -> String = { it.name }
) {
    SettingsSubtitleOfEnabled(
        enabled = enabled.sortedBy { it.ordinal }.map(name),
        allSize = allSize
    )
}
