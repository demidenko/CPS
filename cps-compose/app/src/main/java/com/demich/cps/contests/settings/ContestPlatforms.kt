package com.demich.cps.contests.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.contests.ContestPlatformIcon
import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.loading.ContestsLoaderType
import com.demich.cps.ui.CPSCheckBox
import com.demich.cps.ui.CPSDefaults
import com.demich.cps.ui.CPSIconButton
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.ExpandableSettingsItem
import com.demich.cps.ui.SettingsSubtitleOfEnabled
import com.demich.cps.ui.WordsWithCounterOnOverflow
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.collectItemAsState
import com.demich.cps.utils.context
import com.demich.datastore_itemized.edit
import kotlinx.coroutines.launch

@Composable
internal fun ContestPlatformsSettingsItem() {
    val context = context
    val scope = rememberCoroutineScope()

    val enabledPlatforms by collectItemAsState { context.settingsContests.enabledPlatforms }

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
    ProvideTextStyle(TextStyle(fontSize = 15.sp, color = cpsColors.contentAdditional)) {
        SettingsSubtitleOfEnabled(
            enabled = enabledPlatforms - Contest.Platform.unknown,
            allSize = Contest.platformsExceptUnknown.size
        )
    }
}

@Composable
private fun ContestPlatformsSettingsItemExpandedContent(
    enabledPlatforms: Set<Contest.Platform>,
    onCheckedChange: (Contest.Platform, Boolean) -> Unit
) {
    Column {
        Contest.platformsExceptUnknown.forEach { platform ->
            PlatformCheckRow(
                platform = platform,
                availableLoaders = ContestsLoaderType.entries.filter { platform in it.supportedPlatforms }.toSet(),
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
    availableLoaders: Set<ContestsLoaderType>,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        LeadingIcon(platform = platform)
        Text(
            text = platform.name,
            style = CPSDefaults.MonospaceTextStyle,
            modifier = Modifier.weight(1f)
        )
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
    availableLoaders: Set<ContestsLoaderType>
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
    val resources by collectItemAsState { context.settingsContests.clistAdditionalResources }

    var showDialog by remember { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        LeadingIcon(platform = Contest.Platform.unknown)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "clist additional", style = CPSDefaults.MonospaceTextStyle)
            if (resources.isNotEmpty()) {
                ProvideTextStyle(TextStyle(fontSize = 10.sp, color = cpsColors.contentAdditional)) {
                    WordsWithCounterOnOverflow(words = resources.map { it.name })
                }
            }
        }
        CPSIconButton(icon = CPSIcons.EditList) {
            showDialog = true
        }
    }

    if (showDialog) {
        ClistAdditionalResourcesDialog(
            onDismissRequest = { showDialog = false }
        )
    }
}

@Composable
private fun LeadingIcon(platform: Contest.Platform) =
    ContestPlatformIcon(
        platform = platform,
        size = 28.sp,
        color = cpsColors.content,
        modifier = Modifier.padding(end = 8.dp)
    )