package com.demich.cps.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.demich.cps.ui.CPSFontSize
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.collectItemAsState
import com.demich.datastore_itemized.DataStoreValue

@Composable
fun <T> SettingsContainerScope.SubtitledByValue(
    modifier: Modifier = Modifier,
    item: DataStoreValue<T>,
    title: String,
    subtitle: @Composable (T) -> Unit
) {
    Item(modifier = modifier) {
        val value by collectItemAsState { item }
        Column {
            Text(
                text = title,
                fontSize = CPSFontSize.settingsTitle,
                fontWeight = FontWeight.SemiBold
            )
            ProvideTextStyle(TextStyle(
                fontSize = CPSFontSize.settingsSubtitle,
                color = cpsColors.contentAdditional
            )) {
                subtitle(value)
            }
        }
    }
}