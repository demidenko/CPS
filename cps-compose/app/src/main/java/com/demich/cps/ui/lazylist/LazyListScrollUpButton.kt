package com.demich.cps.ui.lazylist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

    val visible by remember(listState) {
        derivedStateOf {
            listState.lastScrolledBackward && listState.firstVisibleItemIndex > 0
        }
    }

    //TODO:
    // animate alfa on idle (like scrollbar)
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = enter,
        exit = exit
    ) {
        ScrollUpButton(
            backgroundColor = cpsColors.backgroundAdditional,
            onClick = {
                scope.launch { listState.animateScrollToItem(0) }
            }
        )
    }
}

@Composable
private fun ScrollUpButton(
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(backgroundColor)
    ) {
        CPSIconButton(
            icon = CPSIcons.ArrowUp,
            onClick = onClick
        )
    }
}