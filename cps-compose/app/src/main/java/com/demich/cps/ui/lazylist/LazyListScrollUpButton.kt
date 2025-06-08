package com.demich.cps.ui.lazylist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.demich.cps.ui.CPSIconButton
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.theme.cpsColors
import kotlinx.coroutines.launch

@Composable
fun LazyListScrollUpButton(
    modifier: Modifier = Modifier,
    listState: LazyListState,
    enter: EnterTransition,
    exit: ExitTransition
) {
    val scope = rememberCoroutineScope()

    //TODO:
    // hide on user scrolling down
    // animate alfa on idle (like scrollbar)
    AnimatedVisibility(
        visible = listState.firstVisibleItemIndex > 0,
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