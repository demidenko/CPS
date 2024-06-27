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
import com.demich.cps.accounts.managers.AccountManagerType
import com.demich.cps.accounts.managers.allRatedAccountManagers
import com.demich.cps.ui.CPSCheckBox
import com.demich.cps.ui.CPSDefaults
import com.demich.cps.ui.CPSIconButton
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.TextButtonsSelectRow
import com.demich.cps.ui.settingsUI
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberCollect
import com.demich.datastore_itemized.edit
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch


@Composable
internal fun StatusBarButtons() {
    val scope = rememberCoroutineScope()

    val context = context
    val settingsUI = remember { context.settingsUI }

    val coloredStatusBar by rememberCollect { settingsUI.coloredStatusBar.flow }

    val recordedAccountManagers by rememberCollect {
        combine(allRatedAccountManagers.map { it.flowOfInfoWithManager(context) }) { array ->
            array.mapNotNull { it?.manager?.type }
        }.combine(settingsUI.accountsOrder.flow) { managers, order ->
            managers.sortedBy { order.indexOf(it) }
        }
    }

    var showPopup by rememberSaveable { mutableStateOf(false) }
    val resultByMaximum by rememberCollect { settingsUI.statusBarResultByMaximum.flow }
    val disabledManagers by rememberCollect { settingsUI.statusBarDisabledManagers.flow }

    val noneEnabled by remember {
        derivedStateOf {
            recordedAccountManagers.all { it in disabledManagers }
        }
    }

    if (recordedAccountManagers.isNotEmpty()) {
        CPSIconButton(
            icon = CPSIcons.StatusBar,
            onState = coloredStatusBar && !noneEnabled,
        ) {
            if (noneEnabled) {
                showPopup = true
            } else {
                scope.launch { settingsUI.coloredStatusBar(!coloredStatusBar) }
            }
        }
        if (coloredStatusBar) Box {
            CPSIconButton(
                icon = CPSIcons.MoveDown,
                onClick = { showPopup = true }
            )
            StatusBarAccountsPopup(
                expanded = showPopup,
                resultByMaximum = resultByMaximum,
                setResultByMaximum = {
                    scope.launch { settingsUI.statusBarResultByMaximum(it) }
                },
                disabledManagers = disabledManagers,
                onCheckedChange = { type, checked ->
                    scope.launch {
                        settingsUI.statusBarDisabledManagers.edit {
                            if (checked) remove(type) else add(type)
                        }
                    }
                },
                accountManagers = recordedAccountManagers,
                onDismissRequest = { showPopup = false }
            )
        }
    }

}


@Composable
private fun StatusBarAccountsPopup(
    expanded: Boolean,
    resultByMaximum: Boolean,
    setResultByMaximum: (Boolean) -> Unit,
    disabledManagers: Set<AccountManagerType>,
    onCheckedChange: (AccountManagerType, Boolean) -> Unit,
    accountManagers: List<AccountManagerType>,
    onDismissRequest: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = Modifier.background(cpsColors.backgroundAdditional)
    ) {
        accountManagers.forEach { type ->
            Row(modifier = Modifier.padding(end = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                CPSCheckBox(
                    checked = type !in disabledManagers,
                    onCheckedChange = { checked -> onCheckedChange(type, checked) }
                )
                Text(text = type.name, style = CPSDefaults.MonospaceTextStyle)
            }
        }
        TextButtonsSelectRow(
            values = listOf(false, true),
            selectedValue = resultByMaximum,
            text = { value ->
                if (value) "best" else "worst"
            },
            onSelect = setResultByMaximum,
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .align(Alignment.CenterHorizontally)
        )
    }
}