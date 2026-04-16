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
import com.demich.cps.contests.database.toContestPlatform
import com.demich.cps.contests.fetching.ContestsFetchSource
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
import com.demich.datastore_itemized.DataStoreItem
import com.demich.datastore_itemized.edit
import com.demich.kotlin_stdlib_boost.toEnumSet
import kotlinx.coroutines.launch

@Composable
context(scope: SettingsContainerScope)
internal fun ContestPlatformsSettingsItem(
    enabledPlatformsItem: DataStoreItem<Set<Platform>>,
    clistAdditionalResourcesItem: DataStoreItem<List<ClistResource>>
) {
    val scope = rememberCoroutineScope()

    val enabledPlatforms by collectItemAsState { enabledPlatformsItem }
    val clistResources by collectItemAsState { clistAdditionalResourcesItem }

    Expandable(
        title = "Platforms",
        collapsedContent = {
            EnabledPlatformsSubtitle(
                enabledPlatforms = enabledPlatforms,
                clistResources = clistResources
            )
        },
        expandedContent = {
            ContestPlatformsSettingsItemExpandedContent(
                enabledPlatforms = enabledPlatforms,
                clistResources = clistResources,
                onCheckedChange = { platform, checked ->
                    scope.launch {
                        enabledPlatformsItem.edit {
                            if (checked) add(platform) else remove(platform)
                        }
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
    enabledPlatforms: Set<Platform>,
    clistResources: List<ClistResource>,
    onCheckedChange: (Platform, Boolean) -> Unit
) {
    Column {
        contestPlatforms.forEach { platform ->
            PlatformCheckRow(
                platform = platform,
                availableSources = remember(platform) {
                    val platform = platform.toContestPlatform()
                    ContestsFetchSource.entries.filter { platform in it.platforms }.toEnumSet()
                },
                isChecked = platform in enabledPlatforms,
                onCheckedChange = { onCheckedChange(platform, it) }
            )
        }
        ClistAdditionalRow(
            resources = clistResources
        )
    }
}

@Composable
private fun PlatformCheckRow(
    platform: Platform,
    availableSources: Set<ContestsFetchSource>,
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
    platform: Platform,
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
private fun ClistAdditionalRow(
    resources: List<ClistResource>
) {
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