package com.demich.cps.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.ui.dialogs.CPSDialog
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.CPSDataStoreItem
import com.demich.cps.utils.clickableNoRipple
import com.demich.cps.utils.rememberCollect
import kotlinx.coroutines.launch

@Composable
fun SettingsColumn(
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
        modifier = Modifier
            .padding(all = 10.dp)
            .verticalScroll(rememberScrollState())
            .fillMaxWidth()
    )
}

@Composable
fun SettingsItem(
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
fun SettingsItem(
    modifier: Modifier = Modifier,
    title: String,
    description: String = "",
    trailerContent: @Composable () -> Unit
) {
    SettingsItem(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.heightIn(min = 48.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (description.isNotBlank()) {
                    Text(
                        text = description,
                        color = cpsColors.textColorAdditional,
                        fontSize = 14.sp
                    )
                }
            }
            trailerContent()
        }
    }
}

@Composable
fun SettingsSwitchItem(
    checked: Boolean,
    title: String,
    description: String = "",
    onCheckedChange: (Boolean) -> Unit
) {
    SettingsItem(
        title = title,
        description = description
    ) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.padding(start = 5.dp),
            colors = SwitchDefaults.colors(
                checkedThumbColor = cpsColors.colorAccent
            )
        )
    }
}

@Composable
fun SettingsSwitchItem(
    item: CPSDataStoreItem<Boolean>,
    title: String,
    description: String = ""
) {
    val scope = rememberCoroutineScope()
    val checked by rememberCollect { item.flow }
    SettingsSwitchItem(
        checked = checked,
        title = title,
        description = description
    ) {
        scope.launch { item(it) }
    }
}


@Composable
fun<T: Enum<T>> SettingsEnumItem(
    item: CPSDataStoreItem<T>,
    title: String,
    description: String = "",
    optionToString: (T) -> AnnotatedString = { AnnotatedString(it.name) },
    options: List<T>
) {
    val scope = rememberCoroutineScope()
    val selectedOption by rememberCollect { item.flow }

    var showChangeDialog by remember { mutableStateOf(false) }

    SettingsItem(
        title = title,
        description = description,
        modifier = Modifier.clickable {
            showChangeDialog = true
        }
    ) {
        Text(
            text = optionToString(selectedOption),
            fontSize = 18.sp,
            color = cpsColors.colorAccent,
            modifier = Modifier.padding(end = 10.dp)
        )
    }

    if (showChangeDialog) {
        CPSDialog(onDismissRequest = { showChangeDialog = false }) {
            Text(text = title)
            Column(modifier = Modifier.fillMaxWidth()) {
                options.forEach { option ->
                    CPSRadioButtonTitled(
                        title = { Text(text = optionToString(option)) },
                        selected = option == selectedOption,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        scope.launch { item(newValue = option) }
                        showChangeDialog = false
                    }
                }
            }
        }
    }
}


@Composable
fun<T> SettingsItemWithInfo(
    modifier: Modifier = Modifier,
    item: CPSDataStoreItem<T>,
    title: String,
    infoContent: @Composable (T) -> Unit
) {
    SettingsItem(modifier = modifier) {
        val value by rememberCollect { item.flow }
        Column {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            infoContent(value)
        }
    }
}

@Composable
fun SettingsSubtitle(
    text: String
) {
    Text(
        text = text,
        fontSize = 15.sp,
        color = cpsColors.textColorAdditional,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@OptIn(ExperimentalAnimationApi::class)
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
                    imageVector = CPSIcons.Collapse,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickableNoRipple { expanded = !expanded }
                )
                AnimatedContent(
                    targetState = expanded,
                    transitionSpec = { fadeIn() with fadeOut(snap()) }
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