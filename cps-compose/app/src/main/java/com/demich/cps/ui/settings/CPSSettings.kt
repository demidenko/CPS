package com.demich.cps.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.ui.CPSFontSize
import com.demich.cps.ui.IconSp
import com.demich.cps.ui.WordsWithCounterOnOverflow
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.ProvideContentColor
import com.demich.cps.utils.collectItemAsState
import com.demich.datastore_itemized.DataStoreValue


@Composable
inline fun SettingsContainerScope.SettingsSectionHeader(
    title: String,
    painter: Painter,
    modifier: Modifier = Modifier,
    content: SettingsContainerScope.() -> Unit
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProvideContentColor(color = cpsColors.accent) {
            IconSp(
                painter = painter,
                size = 18.sp,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
            Text(
                text = title,
                fontSize = CPSFontSize.settingsSectionTitle,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
    content()
}

@Composable
private inline fun SettingsItem(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .background(cpsColors.backgroundAdditional, RoundedCornerShape(4.dp))
            .fillMaxWidth()
            .padding(all = 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        content()
    }
}

@Composable
fun<T> SettingsItemWithInfo(
    modifier: Modifier = Modifier,
    item: DataStoreValue<T>,
    title: String,
    infoContent: @Composable (T) -> Unit
) {
    SettingsItem(modifier = modifier) {
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
                infoContent(value)
            }
        }
    }
}

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
