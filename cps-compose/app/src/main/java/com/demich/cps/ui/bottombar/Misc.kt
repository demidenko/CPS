package com.demich.cps.ui.bottombar

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.demich.cps.ui.theme.cpsColors


enum class NavigationLayoutType {
    start,  //ABC....
    center, //..ABC..
    evenly  //.A.B.C. (tap area as weight(1f))
}


@Composable
internal fun Scrim(
    show: Boolean,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit
) {
    val alpha by animateFloatAsState(targetValue = if (show) 0.32f else 0f, label = "scrim_alpha")
    if (alpha > 0f) {
        Canvas(modifier = modifier.let {
            if (!show) it
            else it.pointerInput(onDismiss) { detectTapGestures { onDismiss() } }
        }) {
            drawRect(color = Color.Black.copy(alpha = alpha))
        }
    }
}


@Composable
internal fun BottomBarVerticalDivider() {
    Box(
        Modifier
            .fillMaxHeight(0.6f)
            .width(1.dp)
            .background(cpsColors.divider)
    )
}