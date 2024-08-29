package com.demich.cps.ui.topbar

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.demich.cps.ui.CPSIconButton
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.settingsUI
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.collectItemAsState
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberWith
import kotlinx.coroutines.launch


@Composable
internal fun UIPanel(
    modifier: Modifier = Modifier,
    onClosePanel: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val settingsUI = rememberWith(key = context) { settingsUI }

    val useOriginalColors by collectItemAsState { settingsUI.useOriginalColors }
    val darkLightMode by collectItemAsState { settingsUI.darkLightMode }

    Row(modifier = modifier.background(cpsColors.background)) {
        CPSIconButton(icon = CPSIcons.Close, onClick = onClosePanel)
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CPSIconButton(icon = CPSIcons.Colors, onState = useOriginalColors) {
                scope.launch {
                    settingsUI.useOriginalColors(!useOriginalColors)
                }
            }
            StatusBarButtons()
            DarkLightModeButton(
                mode = darkLightMode,
                isSystemInDarkMode = isSystemInDarkTheme()
            ) { mode ->
                scope.launch {
                    settingsUI.darkLightMode(mode)
                }
            }
        }
    }
}
