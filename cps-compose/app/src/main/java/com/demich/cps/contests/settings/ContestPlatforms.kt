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
import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.database.ContestPlatform
import com.demich.cps.contests.database.toGeneralPlatform
import com.demich.cps.contests.database.toGeneralPlatformOrNull
import com.demich.cps.contests.loading.ContestsFetchSource
import com.demich.cps.platforms.Platform
import com.demich.cps.platforms.api.clist.ClistResource
import com.demich.cps.ui.CPSCheckBox
import com.demich.cps.ui.CPSDefaults
import com.demich.cps.ui.CPSIconButton
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.PlatformIcon
import com.demich.cps.ui.WordsWithCounterOnOverflow
import com.demich.cps.ui.settings.Expandable
import com.demich.cps.ui.settings.SettingsContainerScope
import com.demich.cps.ui.settings.Subtitle
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.collectItemAsState
import com.demich.cps.utils.context
import com.demich.kotlin_stdlib_boost.toEnumSet
import kotlinx.coroutines.launch

@Composable
context(scope: SettingsContainerScope)
internal fun ContestPlatformsSettingsItem() {
    val context = context
    val scope = rememberCoroutineScope()

    val enabledPlatforms by collectItemAsState { context.settingsContests.enabledContestPlatforms }
    val clistResources by collectItemAsState { context.settingsContests.clistAdditionalResources }

    Expandable(
        title = "Platforms",
        collapsedContent = {
            EnabledPlatformsSubtitle(
                enabledPlatforms = enabledPlatforms.mapNotNull { it.toGeneralPlatformOrNull() },
                clistResources = clistResources
            )
        },
        expandedContent = {
            ContestPlatformsSettingsItemExpandedContent(
                enabledPlatforms = enabledPlatforms,
                clistResources = clistResources,
                onCheckedChange = { platform, checked ->
                    require(platform != unknown)
                    scope.launch {
                        context.settingsContests.changeEnabled(
                            platform = platform,
                            enabled = checked
                        )
                    }
                }
            )
        }
    )
}

@Composable
context(scope: SettingsContainerScope)
private fun EnabledPlatformsSubtitle(
    enabledPlatforms: Collection<Platform>,
    clistResources: Collection<ClistResource>
) {
    Subtitle(
        selected = remember(key1 = enabledPlatforms, key2 = clistResources) {
            buildList {
                enabledPlatforms.sortedBy { it.ordinal }.forEach { add(it.name) }
                clistResources.forEach { add(it.name) }
            }
        }
    )
}

@Composable
private fun ContestPlatformsSettingsItemExpandedContent(
    enabledPlatforms: Set<ContestPlatform>,
    clistResources: List<ClistResource>,
    onCheckedChange: (ContestPlatform, Boolean) -> Unit
) {
    Column {
        Contest.platformsExceptUnknown.forEach { platform ->
            PlatformCheckRow(
                platform = platform,
                availableSources = ContestsFetchSource.entries.filter { platform in it.platforms }.toEnumSet(),
                isChecked = platform in enabledPlatforms,
                onCheckedChange = { onCheckedChange(platform, it) }
            )
        }
        ClistAdditionalRow(resources = clistResources)
    }
}

@Composable
private fun PlatformCheckRow(
    platform: ContestPlatform,
    availableSources: Set<ContestsFetchSource>,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        LeadingIcon(platform = platform.toGeneralPlatform())
        Text(
            text = platform.name,
            style = CPSDefaults.MonospaceTextStyle,
            modifier = Modifier.weight(1f)
        )
        if (isChecked && availableSources.size > 1) {
            FetchSourcesSetupButton(
                platform = platform,
                fetchSources = availableSources
            )
        }
        CPSCheckBox(
            checked = isChecked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun FetchSourcesSetupButton(
    platform: ContestPlatform,
    fetchSources: Set<ContestsFetchSource>
) {
    var showDialog by remember { mutableStateOf(false) }
    CPSIconButton(icon = CPSIcons.SetupFetchSources) {
        showDialog = true
    }
    if (showDialog) {
        FetchPriorityListDialog(
            platform = platform,
            availableOptions = fetchSources,
            onDismissRequest = { showDialog = false }
        )
    }
}

@Composable
private fun ClistAdditionalRow(resources: List<ClistResource>) {
    var showDialog by remember { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        LeadingIcon(platform = clist)
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
private fun LeadingIcon(platform: Platform) =
    PlatformIcon(
        platform = platform,
        size = 28.sp,
        color = cpsColors.content,
        modifier = Modifier.padding(end = 8.dp)
    )