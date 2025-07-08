package com.demich.cps.ui.settings

import androidx.compose.runtime.Composable


interface SettingsContainerScope {

    @Composable
    fun append(content: @Composable () -> Unit)
}
