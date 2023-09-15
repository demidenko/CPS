package com.demich.cps.ui

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
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

@Composable
fun CPSDropdownMenu(
    menuState: CPSMenuState,
    menuBuilder: CPSMenuBuilder
) {
    CPSDropdownMenu(
        expanded = menuState.expanded,
        onDismissRequest = menuState::close,
        offset = menuState.offset,
        menuBuilder = menuBuilder
    )
}

@Stable
class CPSMenuState(
    private val expandedState: MutableState<Boolean>,
    private val menuOffsetState: MutableState<DpOffset>,
    private val componentSizeState: MutableState<IntSize>,
    private val density: Density
) {
    val expanded by expandedState
    val offset by menuOffsetState

    fun onPlaced(layoutCoordinates: LayoutCoordinates) {
        componentSizeState.value = layoutCoordinates.size
    }

    fun open(pressPosition: Offset) {
        menuOffsetState.value = convert(pressPosition)
        expandedState.value = true
    }

    fun close() {
        expandedState.value = false
    }

    private fun convert(o: Offset): DpOffset =
        with(density) {
            DpOffset(
                x = o.x.toDp(),
                y = (o.y - componentSizeState.value.height).toDp()
            )
        }
}

@Composable
fun rememberMenuState(): CPSMenuState {
    val expanded = remember { mutableStateOf(false) }
    val menuOffset = remember { mutableStateOf(DpOffset.Zero) }
    val componentSize = remember { mutableStateOf(IntSize.Zero) }
    return rememberWith(LocalDensity.current) {
        CPSMenuState(
            expandedState = expanded,
            menuOffsetState = menuOffset,
            componentSizeState = componentSize,
            density = this
        )
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
    val menuState = rememberMenuState()

    LaunchedEffect(interactionSource, menuState) {
        interactionSource.interactions.collect {
            if (it is PressInteraction.Release) {
                menuState.open(it.press.pressPosition)
            }
        }
    }

    Box(
        modifier = modifier
            .onPlaced(menuState::onPlaced)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = { }
            )
    ) {
        content()
        CPSDropdownMenu(
            menuState = menuState,
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
                    .padding(end = 10.dp)
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