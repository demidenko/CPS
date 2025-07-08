package com.demich.cps.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import com.demich.cps.ui.CPSFontSize
import com.demich.cps.ui.WordsWithCounterOnOverflow
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.collectItemAsState
import com.demich.datastore_itemized.DataStoreValue

@Composable
fun <T> SettingsContainerScope.SubtitledByValue(
    modifier: Modifier = Modifier,
    item: DataStoreValue<T>,
    title: String,
    subtitle: @Composable SettingsContainerScope.(T) -> Unit
) {
    Item(modifier = modifier) {
        val value by collectItemAsState { item }
        Column {
            Title(title = title)
            ProvideTextStyle(subtitleTextStyle()) {
                subtitle(value)
            }
        }
    }
}


@Composable
@ReadOnlyComposable
private fun subtitleTextStyle() =
    TextStyle(
        fontSize = CPSFontSize.settingsSubtitle,
        color = cpsColors.contentAdditional
    )

@Composable
fun SettingsContainerScope.Subtitle(
    text: String
) {
    Text(
        text = text,
        style = subtitleTextStyle(),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun SettingsContainerScope.Subtitle(
    selected: Collection<String>
) {
    ProvideTextStyle(value = subtitleTextStyle()) {
        when {
            selected.isEmpty() -> Subtitle(text = "none selected")
            else -> WordsWithCounterOnOverflow(words = selected)
        }
    }
}

@Composable
fun <T: Enum<T>> SettingsContainerScope.Subtitle(
    selected: Collection<T>,
    name: (T) -> String = { it.name }
) {
    Subtitle(selected = selected.sortedBy { it.ordinal }.map(name))
}