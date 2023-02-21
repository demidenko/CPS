package com.demich.cps.ui

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.rememberWith

typealias CPSMenuBuilder = @Composable CPSDropdownMenuScope.() -> Unit

@Composable
fun CPSDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    offset: DpOffset = DpOffset.Zero,
    menuBuilder: CPSMenuBuilder
) {
    DropdownMenu(
        expanded = expanded,
        modifier = Modifier.background(cpsColors.backgroundAdditional),
        offset = offset,
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

//experimental
@Composable
fun ContentWithCPSDropdownMenu(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
    menuBuilder: CPSMenuBuilder
) {
    val interactionSource = remember { MutableInteractionSource() }
    var menuOffset by remember { mutableStateOf(DpOffset.Zero) }
    var expanded by remember { mutableStateOf(false) }

    val convert by rememberWith(LocalDensity.current) {
        mutableStateOf({ s: IntSize, o: Offset ->
            DpOffset(
                x = o.x.toDp(),
                y = (o.y - s.height).toDp()
            )
        })
    }

    Box(
        modifier = modifier
            .indication(interactionSource, LocalIndication.current)
            .pointerInput(convert) {
                detectTapGestures(
                    onTap = {
                        menuOffset = convert(size, it)
                        expanded = true
                    },
                    onPress = { offset ->
                        val press = PressInteraction.Press(offset)
                        interactionSource.emit(press)
                        tryAwaitRelease()
                        interactionSource.emit(PressInteraction.Release(press))
                    }
                )
            }
    ) {
        content()
        CPSDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = menuOffset,
            menuBuilder = menuBuilder
        )
    }
}


@Composable
fun CPSDropdownMenuButton(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    color: Color = cpsColors.content,
    enabled: Boolean = true,
    onState: Boolean = enabled,
    menuBuilder: CPSMenuBuilder
) {
    var showMenu by remember { mutableStateOf(false) }
    ContentWithCPSDropdownMenu(
        expanded = showMenu,
        onDismissRequest = { showMenu = false },
        modifier = modifier,
        content = {
            CPSIconButton(
                icon = icon,
                color = color,
                enabled = enabled,
                onState = onState,
                onClick = { showMenu = true }
            )
        },
        menuBuilder = menuBuilder
    )
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