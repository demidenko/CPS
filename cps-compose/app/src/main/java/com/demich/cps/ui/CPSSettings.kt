package com.demich.cps.ui

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.ui.dialogs.CPSDialogSelect
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.ProvideContentColor
import com.demich.cps.utils.clickableNoRipple
import com.demich.cps.utils.collectItemAsState
import com.demich.cps.utils.context
import com.demich.cps.workers.CPSPeriodicWork
import com.demich.cps.workers.ProfilesWorker
import com.demich.datastore_itemized.DataStoreItem
import kotlinx.coroutines.launch

@Composable
inline fun SettingsColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val border: Dp = 10.dp
    //spaceBy adds space only between items but start + end required too
    Column(
        verticalArrangement = Arrangement.spacedBy(border),
        modifier = modifier
            .padding(horizontal = border)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.fillMaxWidth())
        content()
        Spacer(modifier = Modifier.fillMaxWidth())
    }
}

@Composable
inline fun SettingsColumn(
    requiredNotificationsPermission: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier) {
        SettingsColumn(content = content, modifier = Modifier.weight(1f))
        NotificationsPermissionPanel(permissionRequired = requiredNotificationsPermission)
    }
}

@Composable
inline fun ColumnScope.SettingsSectionHeader(
    title: String,
    painter: Painter,
    modifier: Modifier = Modifier,
    content: ColumnScope.() -> Unit
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
                fontSize = CPSDefaults.settingsSectionTitle,
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
                fontSize = CPSDefaults.settingsTitle,
                fontWeight = FontWeight.SemiBold
            )
            if (description.isNotBlank()) {
                Text(
                    text = description,
                    color = cpsColors.contentAdditional,
                    fontSize = CPSDefaults.settingsDescription
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
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.padding(start = 5.dp),
            colors = SwitchDefaults.colors(
                checkedThumbColor = cpsColors.accent
            )
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
fun SettingsSwitchItem(
    item: DataStoreItem<Boolean>,
    title: String,
    description: String = "",
    onCheckedChange: (Boolean) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val checked by collectItemAsState { item }
    SettingsSwitchItem(
        checked = checked,
        title = title,
        description = description
    ) {
        scope.launch {
            item(it)
            onCheckedChange(it)
        }
    }
}

@Composable
fun SettingsSwitchItemWithWork(
    item: DataStoreItem<Boolean>,
    title: String,
    description: String = "",
    workGetter: (Context) -> CPSPeriodicWork,
    stopWorkOnUnchecked: Boolean = true
) {
    val context = context
    val scope = rememberCoroutineScope()
    SettingsSwitchItem(
        item = item,
        title = title,
        description = description
    ) { checked ->
        scope.launch {
            with(workGetter(context)) {
                if (checked) startImmediate()
                else if (stopWorkOnUnchecked) stop()
            }
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
        workGetter = ProfilesWorker::getWork,
        stopWorkOnUnchecked = false
    )
}

@Composable
fun<T: Enum<T>> SettingsEnumItemContent(
    item: DataStoreItem<T>,
    title: String,
    description: String = "",
    optionToString: @Composable (T) -> AnnotatedString = { AnnotatedString(it.name) },
    options: List<T>
) {
    val scope = rememberCoroutineScope()
    val selectedOption by collectItemAsState { item }

    var showChangeDialog by rememberSaveable { mutableStateOf(false) }

    SettingsItemContent(
        title = title,
        description = description
    ) {
        TextButton(onClick = { showChangeDialog = true }) {
            Text(
                text = optionToString(selectedOption),
                fontSize = CPSDefaults.settingsTitle,
                color = cpsColors.accent
            )
        }
    }

    if (showChangeDialog) {
        CPSDialogSelect(
            title = title,
            options = options,
            selectedOption = selectedOption,
            optionTitle = { Text(text = optionToString(it)) },
            onDismissRequest = { showChangeDialog = false },
            onSelectOption = {
                scope.launch { item(newValue = it) }
            }
        )
    }
}

@Composable
fun<T: Enum<T>> SettingsEnumItem(
    item: DataStoreItem<T>,
    title: String,
    description: String = "",
    optionToString: @Composable (T) -> AnnotatedString = { AnnotatedString(it.name) },
    options: List<T>
) {
    SettingsItem {
        SettingsEnumItemContent(
            item = item,
            title = title,
            description = description,
            optionToString = optionToString,
            options = options
        )
    }
}


@Composable
fun<T> SettingsItemWithInfo(
    modifier: Modifier = Modifier,
    item: DataStoreItem<T>,
    title: String,
    infoContent: @Composable (T) -> Unit
) {
    SettingsItem(modifier = modifier) {
        val value by collectItemAsState { item }
        Column {
            Text(
                text = title,
                fontSize = CPSDefaults.settingsTitle,
                fontWeight = FontWeight.SemiBold
            )
            ProvideTextStyle(TextStyle(
                fontSize = CPSDefaults.settingsSubtitle,
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
        fontSize = CPSDefaults.settingsSubtitle,
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

@Composable
fun ExpandableSettingsItem(
    title: String,
    collapsedContent: @Composable () -> Unit,
    expandedContent: @Composable () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    SettingsItem(
        modifier = Modifier.clickableNoRipple(enabled = !expanded) { expanded = true }
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(animationSpec = snap()),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = CPSIcons.CollapseUp,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    fontSize = CPSDefaults.settingsTitle,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickableNoRipple { expanded = !expanded }
                )
                AnimatedContent(
                    targetState = expanded,
                    transitionSpec = { fadeIn() togetherWith fadeOut(snap()) },
                    modifier = Modifier.fillMaxWidth(),
                    label = "settings item content"
                ) { itemExpanded ->
                    if (itemExpanded) {
                        Box(
                            content = { expandedContent() },
                            modifier = Modifier
                                .padding(horizontal = 10.dp)
                                .padding(top = 8.dp)
                        )
                    } else {
                        collapsedContent()
                    }
                }
            }
        }
    }
}