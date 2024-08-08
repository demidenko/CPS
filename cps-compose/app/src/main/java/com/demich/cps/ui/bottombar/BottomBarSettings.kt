package com.demich.cps.ui.bottombar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Divider
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.ui.ButtonsSelectRow
import com.demich.cps.ui.CPSDefaults
import com.demich.cps.ui.CPSIconButton
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.settingsUI
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberCollect
import kotlinx.coroutines.launch

@Composable
internal fun BottomBarSettings(
    modifier: Modifier = Modifier,
    onCloseRequest: () -> Unit
) {
    Column(modifier = modifier) {
        CloseRow(onCloseRequest)
        Divider()
        LayoutSelectRow()
    }
}

@Composable
private fun CloseRow(
    onCloseRequest: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        CPSIconButton(
            icon = CPSIcons.Close,
            onClick = onCloseRequest,
            color = cpsColors.contentAdditional,
            modifier = Modifier.align(Alignment.TopEnd)
        )
    }
}

@Composable
private fun LayoutSelectRow() {
    val context = context
    val scope = rememberCoroutineScope()
    val layoutType by rememberCollect { context.settingsUI.navigationLayoutType.flow }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "layout: ",
            fontSize = 14.sp,
            color = cpsColors.contentAdditional,
            style = CPSDefaults.MonospaceTextStyle
        )
        ButtonsSelectRow(
            values = NavigationLayoutType.entries,
            selectedValue = layoutType,
            onSelect = {
                scope.launch {
                    context.settingsUI.navigationLayoutType(it)
                }
            }
        ) {
            DemoRow(it, Modifier.widthIn(max = 56.dp))
        }
    }

}

@Composable
private fun DemoRow(
    navigationLayoutType: NavigationLayoutType,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current
) {
    BottomBarNavigationItems(
        navigationLayoutType = navigationLayoutType,
        modifier = modifier
            .height(18.dp)
            .border(width = 1.dp, color = color)
            .padding(all = 4.dp)
    ) {
        repeat(3) {
            Canvas(modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1f, matchHeightConstraintsFirst = true)
                .padding(all = 1.dp)
            ) {
                drawCircle(color = color)
            }
        }
    }
}