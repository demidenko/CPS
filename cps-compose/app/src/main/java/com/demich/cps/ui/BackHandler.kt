package com.demich.cps.ui

import androidx.compose.runtime.Composable

@Composable
fun BackHandler(
    enabled: () -> Boolean,
    onBackPressed: () -> Unit,
    content: @Composable () -> Unit
) {
    content()
    androidx.activity.compose.BackHandler(
        enabled = enabled(),
        onBack = onBackPressed
    )
}