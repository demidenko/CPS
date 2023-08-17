package com.demich.cps.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.demich.cps.NavigationLayoutType
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberCollect
import kotlinx.coroutines.launch

@Composable
fun BottomBarSettings(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit
) {
    val context = context
    val scope = rememberCoroutineScope()
    val layoutType by rememberCollect { context.settingsUI.navigationLayoutType.flow }

    Box(modifier = modifier) {
        TextButtonsSelectRow(
            modifier = Modifier.align(Alignment.TopStart),
            values = NavigationLayoutType.entries,
            selectedValue = layoutType,
            text = { it.name },
            onSelect = {
                scope.launch {
                    context.settingsUI.navigationLayoutType(it)
                }
            }
        )

        TextButton(
            onClick = onDismissRequest,
            Modifier.align(Alignment.TopEnd)
        ) {
            Text(text = "close")
        }
    }
}