package com.demich.cps.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.demich.cps.ui.theme.cpsColors

@Composable
fun SettingsColumn(
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 10.dp)
            .fillMaxWidth(),
        content = content
    )
}

@Composable
fun ColumnScope.SettingsItem(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Spacer(modifier = Modifier.height(10.dp).fillMaxWidth())
    Box(
        modifier = modifier
            .background(cpsColors.backgroundAdditional)
            .fillMaxWidth()
            .padding(all = 10.dp)
    ) {
        content()
    }
}