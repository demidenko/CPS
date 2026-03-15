package com.demich.cps.ui.topbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.DropdownMenu
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.demich.cps.profiles.managers.ProfileManager
import com.demich.cps.profiles.managers.ProfilePlatform
import com.demich.cps.profiles.managers.flowOfExisted
import com.demich.cps.profiles.managers.platform
import com.demich.cps.ui.CPSCheckBox
import com.demich.cps.ui.CPSDefaults
import com.demich.cps.ui.CPSIconButton
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.TextButtonsSelectRow
import com.demich.cps.ui.UISettingsDataStore
import com.demich.cps.ui.settingsUI
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.collectAsState
import com.demich.cps.utils.collectItemAsState
import com.demich.cps.utils.context
import com.demich.datastore_itemized.edit
import com.demich.datastore_itemized.setValueIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch


@Composable
internal fun StatusBarButtons(
    modifier: Modifier = Modifier
) {
    val context = context
    val settingsUI = remember { context.settingsUI }

    val disabledPlatforms by collectItemAsState { settingsUI.statusBarDisabledPlatforms }
    val coloredStatusBar by collectItemAsState { settingsUI.coloredStatusBar }
    val rankSelector by collectItemAsState { settingsUI.statusBarRankSelector }

    val recordedPlatforms by collectAsState {
        ProfileManager.ratedEntries().flowOfExisted(context)
            .map { it.map { it.platform } }
            .combine(settingsUI.profilesOrder.asFlow()) { platforms, order ->
                platforms.sortedBy { order.indexOf(it) }
            }
    }

    val scope = rememberCoroutineScope()
    recordedPlatforms.let { platforms ->
        if (platforms.isNotEmpty()) {
            StatusBarButtons(
                modifier = modifier,
                coloredStatusBar = coloredStatusBar,
                rankSelector = rankSelector,
                platforms = platforms,
                disabledPlatforms = disabledPlatforms,
                onSetEnabled = { settingsUI.coloredStatusBar.setValueIn(scope, it) },
                onSetRankSelector = { settingsUI.statusBarRankSelector.setValueIn(scope, it) },
                onCheckedChange = { platform, checked ->
                    scope.launch {
                        settingsUI.statusBarDisabledPlatforms.edit {
                            if (checked) remove(platform) else add(platform)
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun StatusBarButtons(
    modifier: Modifier = Modifier,
    coloredStatusBar: Boolean,
    rankSelector: UISettingsDataStore.StatusBarRankSelector,
    platforms: List<ProfilePlatform>,
    disabledPlatforms: Set<ProfilePlatform>,
    onSetEnabled: (Boolean) -> Unit,
    onSetRankSelector: (UISettingsDataStore.StatusBarRankSelector) -> Unit,
    onCheckedChange: (ProfilePlatform, Boolean) -> Unit
) {
    var showPopup by rememberSaveable { mutableStateOf(false) }
    val noneEnabled = platforms.all { it in disabledPlatforms }

    Row(modifier = modifier) {
        CPSIconButton(
            icon = CPSIcons.StatusBar,
            onState = coloredStatusBar && !noneEnabled,
        ) {
            if (noneEnabled) {
                showPopup = true
            } else {
                onSetEnabled(!coloredStatusBar)
            }
        }

        if (coloredStatusBar) Box {
            CPSIconButton(
                icon = CPSIcons.MoveDown,
                onClick = { showPopup = true }
            )
            StatusBarPlatformsPopup(
                expanded = showPopup,
                rankSelector = rankSelector,
                onSetRankSelector = onSetRankSelector,
                disabledPlatforms = disabledPlatforms,
                onCheckedChange = onCheckedChange,
                platforms = platforms,
                onDismissRequest = { showPopup = false }
            )
        }
    }
}


@Composable
private fun StatusBarPlatformsPopup(
    expanded: Boolean,
    rankSelector: UISettingsDataStore.StatusBarRankSelector,
    onSetRankSelector: (UISettingsDataStore.StatusBarRankSelector) -> Unit,
    disabledPlatforms: Set<ProfilePlatform>,
    onCheckedChange: (ProfilePlatform, Boolean) -> Unit,
    platforms: List<ProfilePlatform>,
    onDismissRequest: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = Modifier.background(cpsColors.backgroundAdditional)
    ) {
        platforms.forEach { platform ->
            Row(modifier = Modifier.padding(end = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                CPSCheckBox(
                    checked = platform !in disabledPlatforms,
                    onCheckedChange = { checked -> onCheckedChange(platform, checked) }
                )
                Text(text = platform.name, style = CPSDefaults.MonospaceTextStyle)
            }
        }
        TextButtonsSelectRow(
            values = UISettingsDataStore.StatusBarRankSelector.entries,
            selectedValue = rankSelector,
            text = { value ->
                when (value) {
                    Min -> "worst"
                    Max -> "best"
                }
            },
            onSelect = onSetRankSelector,
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .align(Alignment.CenterHorizontally)
        )
    }
}