package com.demich.cps.ui.topbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.DropdownMenu
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.demich.cps.accounts.managers.AccountManager
import com.demich.cps.accounts.managers.ProfilePlatform
import com.demich.cps.accounts.managers.flowWithProfileResult
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
import kotlinx.coroutines.launch


@Composable
internal fun StatusBarButtons() {
    val scope = rememberCoroutineScope()

    val context = context
    val settingsUI = remember { context.settingsUI }

    val coloredStatusBar by collectItemAsState { settingsUI.coloredStatusBar }

    val recordedPlatforms by collectAsState {
        combine(AccountManager.ratedEntries().map { it.flowWithProfileResult(context) }) { array ->
            array.mapNotNull { it?.manager?.platform }
        }.combine(settingsUI.profilesOrder.asFlow()) { platforms, order ->
            platforms.sortedBy { order.indexOf(it) }
        }
    }

    var showPopup by rememberSaveable { mutableStateOf(false) }
    val rankSelector by collectItemAsState { settingsUI.statusBarRankSelector }
    val disabledPlatforms by collectItemAsState { settingsUI.statusBarDisabledPlatforms }

    val noneEnabled by remember {
        derivedStateOf {
            recordedPlatforms.all { it in disabledPlatforms }
        }
    }

    if (recordedPlatforms.isNotEmpty()) {
        CPSIconButton(
            icon = CPSIcons.StatusBar,
            onState = coloredStatusBar && !noneEnabled,
        ) {
            if (noneEnabled) {
                showPopup = true
            } else {
                settingsUI.coloredStatusBar.setValueIn(scope, !coloredStatusBar)
            }
        }
        if (coloredStatusBar) Box {
            CPSIconButton(
                icon = CPSIcons.MoveDown,
                onClick = { showPopup = true }
            )
            StatusBarAccountsPopup(
                expanded = showPopup,
                rankSelector = rankSelector,
                onSetRankSelector = {
                    settingsUI.statusBarRankSelector.setValueIn(scope, it)
                },
                disabledPlatforms = disabledPlatforms,
                onCheckedChange = { platform, checked ->
                    scope.launch {
                        settingsUI.statusBarDisabledPlatforms.edit {
                            if (checked) remove(platform) else add(platform)
                        }
                    }
                },
                platforms = recordedPlatforms,
                onDismissRequest = { showPopup = false }
            )
        }
    }

}


@Composable
private fun StatusBarAccountsPopup(
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