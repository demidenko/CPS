package com.demich.cps.ui.topbar

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.demich.cps.ui.CPSIconButton
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.settingsUI
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.ui.uiSpecs
import com.demich.cps.utils.backgroundCoroutineScope
import com.demich.cps.utils.collectAsStateWithNull
import com.demich.cps.utils.context
import com.demich.cps.utils.onNotNull
import com.demich.datastore_itemized.setValueIn


@Composable
internal fun UIPanel(
    modifier: Modifier = Modifier,
    onClosePanel: () -> Unit
) {
    Row(modifier = modifier.background(cpsColors.background)) {
        CPSIconButton(
            icon = CPSIcons.Close,
            onClick = onClosePanel
        )
        Buttons(
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun Buttons(
    modifier: Modifier = Modifier
) {
    val scope = backgroundCoroutineScope

    val context = context
    val settingsUI = remember { context.settingsUI }

    val specsState = remember { settingsUI.uiSpecs }.collectAsStateWithNull()
    specsState.onNotNull { specs ->
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CPSIconButton(
                icon = CPSIcons.Colors,
                onState = specs.usePlatformColors,
                onClick = {
                    settingsUI.usePlatformColors.setValueIn(scope, !specs.usePlatformColors)
                }
            )
            StatusBarButtons()
            DarkLightModeButton(
                mode = specs.darkLightMode,
                isSystemInDarkMode = isSystemInDarkTheme(),
                onModeChanged = { mode ->
                    settingsUI.darkLightMode.setValueIn(scope, mode)
                }
            )
        }
    }
}