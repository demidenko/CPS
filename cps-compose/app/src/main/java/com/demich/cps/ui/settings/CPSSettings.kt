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
import androidx.compose.material.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.ui.CPSFontSize
import com.demich.cps.ui.CPSSwitch
import com.demich.cps.ui.IconSp
import com.demich.cps.ui.WordsWithCounterOnOverflow
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.ProvideContentColor
import com.demich.cps.utils.collectItemAsState
import com.demich.cps.utils.context
import com.demich.cps.workers.CPSPeriodicWorkProvider
import com.demich.cps.workers.ProfilesWorker
import com.demich.datastore_itemized.DataStoreItem
import com.demich.datastore_itemized.DataStoreValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


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
inline fun SettingsItem(
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
inline fun SettingsItemContent(
    title: String,
    description: String = "",
    trailerContent: @Composable () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.minimumInteractiveComponentSize()
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = CPSFontSize.settingsTitle,
                fontWeight = FontWeight.SemiBold
            )
            if (description.isNotBlank()) {
                Text(
                    text = description,
                    color = cpsColors.contentAdditional,
                    fontSize = CPSFontSize.settingsDescription
                )
            }
        }
        trailerContent()
    }
}

@Composable
inline fun SettingsItem(
    modifier: Modifier = Modifier,
    title: String,
    description: String = "",
    trailerContent: @Composable () -> Unit
) {
    SettingsItem(modifier = modifier) {
        SettingsItemContent(
            title = title,
            description = description,
            trailerContent = trailerContent
        )
    }
}

@Composable
fun SettingsSwitchItemContent(
    checked: Boolean,
    title: String,
    description: String = "",
    onCheckedChange: (Boolean) -> Unit
) {
    SettingsItemContent(
        title = title,
        description = description
    ) {
        CPSSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.padding(start = 5.dp)
        )
    }
}

@Composable
fun SettingsSwitchItem(
    checked: Boolean,
    title: String,
    description: String = "",
    onCheckedChange: (Boolean) -> Unit
) {
    SettingsItem {
        SettingsSwitchItemContent(
            checked = checked,
            title = title,
            description = description,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private inline fun SettingsSwitchItem(
    item: DataStoreItem<Boolean>,
    title: String,
    description: String = "",
    crossinline onCheckedChange: suspend CoroutineScope.(Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()
    val checked by collectItemAsState { item }
    SettingsSwitchItem(
        checked = checked,
        title = title,
        description = description
    ) {
        scope.launch {
            item.setValue(it)
            onCheckedChange(it)
        }
    }
}

@Composable
fun SettingsSwitchItem(
    item: DataStoreItem<Boolean>,
    title: String,
    description: String = ""
) {
    SettingsSwitchItem(
        item = item,
        title = title,
        description = description,
        onCheckedChange = { }
    )
}

@Composable
fun SettingsSwitchItemWithWork(
    item: DataStoreItem<Boolean>,
    title: String,
    description: String = "",
    workProvider: CPSPeriodicWorkProvider,
    stopWorkOnUnchecked: Boolean = true
) {
    val context = context
    SettingsSwitchItem(
        item = item,
        title = title,
        description = description
    ) { checked ->
        with(workProvider.getWork(context)) {
            if (checked) startImmediate()
            else if (stopWorkOnUnchecked) stop()
        }
    }
}

@Composable
fun SettingsSwitchItemWithProfilesWork(
    item: DataStoreItem<Boolean>,
    title: String,
    description: String = ""
) {
    SettingsSwitchItemWithWork(
        item = item,
        title = title,
        description = description,
        workProvider = ProfilesWorker,
        stopWorkOnUnchecked = false
    )
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
