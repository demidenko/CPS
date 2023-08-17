package com.demich.cps.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.dp
import com.demich.cps.NavigationLayoutType
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberCollect
import kotlinx.coroutines.launch

data object BottomBarHeaderLayoutId

@Composable
fun BottomBarSettings(
    backgroundColor: Color = cpsColors.backgroundNavigation,
    onDismissRequest: () -> Unit
) {
    Surface(
        modifier = Modifier.layoutId(BottomBarHeaderLayoutId),
        color = backgroundColor
    ) {
        BottomBarSettingsContent(
            onDismissRequest = onDismissRequest
        )
    }
}

@Composable
private fun BottomBarSettingsContent(
    onDismissRequest: () -> Unit
) {
    val context = context
    val scope = rememberCoroutineScope()
    val layoutType by rememberCollect { context.settingsUI.navigationLayoutType.flow }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(all = 8.dp)
    ) {
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