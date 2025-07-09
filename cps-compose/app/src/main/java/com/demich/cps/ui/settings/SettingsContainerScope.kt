package com.demich.cps.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier


interface SettingsContainerScope {
    @Composable
    fun append(
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit
    )
}
