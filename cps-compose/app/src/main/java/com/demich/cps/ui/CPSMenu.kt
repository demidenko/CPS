package com.demich.cps.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.demich.cps.ui.theme.cpsColors

typealias CPSMenuBuilder = @Composable CPSDropdownMenuScope.() -> Unit

@Composable
fun CPSDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    menuBuilder: CPSMenuBuilder
) {
    DropdownMenu(
        expanded = expanded,
        modifier = Modifier.background(cpsColors.backgroundAdditional),
        onDismissRequest = onDismissRequest
    ) {
        CPSDropdownMenuScope(onDismissRequest).menuBuilder()
    }
}

@Composable
fun ContentWithCPSDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    menuAlignment: Alignment = Alignment.Center,
    content: @Composable () -> Unit,
    menuBuilder: CPSMenuBuilder
) {
    Box(modifier = modifier) {
        content()
        Box(modifier = Modifier.align(alignment = menuAlignment)) {
            CPSDropdownMenu(
                expanded = expanded,
                onDismissRequest = onDismissRequest,
                menuBuilder = menuBuilder
            )
        }
    }
}

@Stable
class CPSDropdownMenuScope(private val menuDismissRequest: () -> Unit) {
    @Composable
    fun CPSDropdownMenuItem(
        title: @Composable () -> Unit,
        icon: ImageVector,
        enabled: Boolean = true,
        onClick: () -> Unit
    ) = DropdownMenuItem(
        enabled = enabled,
        onClick = {
            menuDismissRequest()
            onClick()
        },
        contentPadding = PaddingValues(start = 16.dp, end = 26.dp),
        content = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(26.dp)
            )
            title()
        }
    )

    @Composable
    fun CPSDropdownMenuItem(
        title: String,
        icon: ImageVector,
        enabled: Boolean = true,
        onClick: () -> Unit
    ) = CPSDropdownMenuItem(
        title = { Text(text = title) },
        icon = icon,
        enabled = enabled,
        onClick = onClick
    )
}