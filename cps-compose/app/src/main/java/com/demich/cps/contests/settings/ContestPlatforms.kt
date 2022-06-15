package com.demich.cps.contests.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.contests.Contest
import com.demich.cps.contests.ContestPlatformIcon
import com.demich.cps.contests.loaders.ContestsLoaders
import com.demich.cps.ui.*
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.context
import com.demich.cps.utils.edit
import com.demich.cps.utils.rememberCollect
import kotlinx.coroutines.launch

@Composable
fun ContestPlatformsSettingsItem() {
    val context = context
    val scope = rememberCoroutineScope()

    val settings = remember { context.settingsContests }
    val enabledPlatforms by rememberCollect { settings.enabledPlatforms.flow }

    ExpandableSettingsItem(
        title = "Platforms",
        collapsedContent = { ContestPlatformsSettingsItemContent(enabledPlatforms) },
        expandedContent = {
            ContestPlatformsSettingsItemExpandedContent(
                enabledPlatforms = enabledPlatforms,
                onCheckedChange = { platform, checked ->
                    require(platform != Contest.Platform.unknown)
                    scope.launch {
                        context.settingsContests.enabledPlatforms.edit {
                            if (checked) add(platform) else remove(platform)
                        }
                    }
                }
            )
        }
    )
}

@Composable
private fun ContestPlatformsSettingsItemContent(
    enabledPlatforms: Set<Contest.Platform>
) {
    val platforms = enabledPlatforms - Contest.Platform.unknown
    val text = when {
        platforms.isEmpty() -> "none selected"
        platforms.size == Contest.platformsExceptUnknown.size -> "all selected"
        platforms.size < 4 -> platforms.joinToString()
        else -> platforms.toList().let { "${it[0]}, ${it[1]} and ${it.size - 2} more" }
    }
    SettingsSubtitle(text = text)
}

@Composable
private fun ContestPlatformsSettingsItemExpandedContent(
    enabledPlatforms: Set<Contest.Platform>,
    onCheckedChange: (Contest.Platform, Boolean) -> Unit
) {
    Column {
        Contest.platformsExceptUnknown.forEach { platform ->
            val availableLoaders = ContestsLoaders.values().filter { platform in it.supportedPlatforms }.toSet()
            PlatformCheckRow(
                platform = platform,
                availableLoaders = availableLoaders,
                isChecked = platform in enabledPlatforms,
                onCheckedChange = { onCheckedChange(platform, it) }
            )
        }
        ClistAdditionalRow()
    }
}

@Composable
private fun PlatformCheckRow(
    platform: Contest.Platform,
    availableLoaders: Set<ContestsLoaders>,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ContestPlatformIcon(
            platform = platform,
            size = 28.sp,
            color = cpsColors.content,
            modifier = Modifier.padding(end = 8.dp)
        )
        MonospacedText(text = platform.name, modifier = Modifier.weight(1f))
        if (isChecked && availableLoaders.size > 1) {
            LoadersSetupButton(
                platform = platform,
                availableLoaders = availableLoaders
            )
        }
        CPSCheckBox(
            checked = isChecked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun LoadersSetupButton(
    platform: Contest.Platform,
    availableLoaders: Set<ContestsLoaders>
) {
    var showDialog by remember { mutableStateOf(false) }
    CPSIconButton(icon = CPSIcons.SetupLoaders) {
        showDialog = true
    }
    if (showDialog) {
        LoadersPriorityListDialog(
            platform = platform,
            availableOptions = availableLoaders,
            onDismissRequest = { showDialog = false }
        )
    }
}

@Composable
private fun ClistAdditionalRow() {
    val context = context
    val settings = remember(context) { context.settingsContests }
    val resources by rememberCollect { settings.clistAdditionalResources.flow }

    var showDialog by remember { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        ContestPlatformIcon(
            platform = Contest.Platform.unknown,
            size = 28.sp,
            color = cpsColors.content,
            modifier = Modifier.padding(end = 8.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            MonospacedText(text = "clist additional")
            if (resources.isNotEmpty()) {
                Text(
                    text = resources.joinToString { it.name },
                    fontSize = 10.sp,
                    color = cpsColors.contentAdditional,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        CPSIconButton(icon = CPSIcons.EditList) {
            showDialog = true
        }
    }

    if (showDialog) {
        ClistAdditionalResourcesDialog(item = settings.clistAdditionalResources) {
            showDialog = false
        }
    }
}