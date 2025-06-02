package com.demich.cps.ui.lazylist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.demich.cps.ui.CPSIconButton
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.theme.cpsColors
import kotlinx.coroutines.launch

@Composable
fun LazyListScrollUpButton(
    modifier: Modifier = Modifier,
    listState: LazyListState
) {
    val scope = rememberCoroutineScope()

    val firstIndex by remember(listState) {
        derivedStateOf { listState.firstVisibleItemIndex }
    }

    //TODO: hide on scrolling to top, animate alfa
    if (firstIndex > 0) {
        ScrollUpButton(modifier) {
            scope.launch { listState.animateScrollToItem(0) }
        }
    }
}

@Composable
private fun ScrollUpButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    //TODO: finish
    Box(
        modifier = modifier
            .size(48.dp)
            .background(cpsColors.accent)
    ) {
        CPSIconButton(icon = CPSIcons.MoveUp, onClick = onClick)
    }
}