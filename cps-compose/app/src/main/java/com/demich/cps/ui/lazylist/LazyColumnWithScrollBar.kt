package com.demich.cps.ui.lazylist

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.demich.cps.ui.CPSDefaults
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.ifThen

@Composable
fun LazyColumnWithScrollBar(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    scrollBarEnabled: Boolean = true,
    content: LazyListScope.() -> Unit
) {
    LazyColumn(
        modifier = modifier.ifThen(scrollBarEnabled) {
            drawScrollBar(
                state = state,
                activeColor = cpsColors.content.copy(alpha = 0.6f),
                inactiveColor = cpsColors.content.copy(alpha = 0.37f),
                scrollBarWidth = CPSDefaults.scrollBarWidth,
                minimumScrollBarHeight = 10.dp,
                enterAnimationSpec = snap(),
                exitAnimationSpec = tween(delayMillis = 600, easing = LinearEasing)
            )
        },
        state = state,
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
        content = content
    )
}
