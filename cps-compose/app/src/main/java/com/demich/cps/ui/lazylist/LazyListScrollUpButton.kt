package com.demich.cps.ui.lazylist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import com.demich.cps.ui.CPSIconButton
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.theme.cpsColors
import kotlinx.coroutines.launch

@Composable
fun LazyListScrollUpButton(
    modifier: Modifier = Modifier,
    listState: LazyListState,
    scrollListenerState: HorizontalScrollListenerState,
    enter: EnterTransition,
    exit: ExitTransition
) {
    val scope = rememberCoroutineScope()

    //TODO:
    // animate alfa on idle (like scrollbar)
    AnimatedVisibility(
        visible = scrollListenerState.scrolledUp && listState.firstVisibleItemIndex > 0,
        modifier = modifier,
        enter = enter,
        exit = exit
    ) {
        ScrollUpButton(
            onClick = {
                scope.launch { listState.animateScrollToItem(0) }
            }
        )
    }
}


class HorizontalScrollListenerState: NestedScrollConnection {
    private var lastY by mutableFloatStateOf(0f)
    val scrolledUp: Boolean get() = lastY > 0

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        lastY = consumed.y
        return super.onPostScroll(consumed, available, source)
    }
}

@Composable
fun rememberScrollListenerState(): HorizontalScrollListenerState {
    return remember { HorizontalScrollListenerState() }
}

@Composable
private fun ScrollUpButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(cpsColors.backgroundAdditional)
    ) {
        CPSIconButton(
            icon = CPSIcons.ArrowUp,
            onClick = onClick
        )
    }
}