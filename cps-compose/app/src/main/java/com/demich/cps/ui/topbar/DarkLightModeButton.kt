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
            DarkLightMode.SYSTEM -> CPSIcons.DarkLightAuto
            else -> CPSIcons.DarkLight
        },
        onClick = {
            onModeChanged(
                when (mode) {
                    DarkLightMode.SYSTEM -> if (isSystemInDarkMode) DarkLightMode.LIGHT else DarkLightMode.DARK
                    DarkLightMode.DARK -> DarkLightMode.LIGHT
                    DarkLightMode.LIGHT -> DarkLightMode.DARK
                }
            )
        }
    )
}