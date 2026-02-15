package com.demich.cps.ui.topbar

import androidx.compose.runtime.Composable
import com.demich.cps.ui.CPSIconButton
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.theme.DarkLightMode


@Composable
internal fun DarkLightModeButton(
    mode: DarkLightMode,
    isSystemInDarkMode: Boolean,
    onModeChanged: (DarkLightMode) -> Unit
) {
    CPSIconButton(
        icon = when (mode) {
            SYSTEM -> CPSIcons.DarkLightAuto
            else -> CPSIcons.DarkLight
        },
        onClick = {
            onModeChanged(
                when (mode) {
                    SYSTEM -> if (isSystemInDarkMode) LIGHT else DARK
                    DARK -> LIGHT
                    LIGHT -> DARK
                }
            )
        }
    )
}