package com.demich.cps.ui.bottombar

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.demich.cps.ui.CPSIconButton
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.TextButtonsSelectRow
import com.demich.cps.ui.settingsUI
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberCollect
import kotlinx.coroutines.launch

@Composable
internal fun BottomBarSettings(
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

        CPSIconButton(
            icon = CPSIcons.Close,
            onClick = onDismissRequest,
            modifier = Modifier.align(Alignment.TopEnd)
        )
    }
}