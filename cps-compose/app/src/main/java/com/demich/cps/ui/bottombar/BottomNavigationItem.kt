package com.demich.cps.ui.bottombar

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Indication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.demich.cps.ui.theme.cpsColors


@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CPSBottomNavigationItem(
    icon: ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    indication: Indication?,
    onLongPress: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val fraction by animateFloatAsState(targetValue = if (isSelected) 1f else 0f)

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .combinedClickable(
                indication = indication,
                interactionSource = null,
                onClick = onClick,
                onLongClick = onLongPress
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = lerp(start = cpsColors.content, stop = cpsColors.accent, fraction),
            modifier = Modifier
                .align(Alignment.Center)
                .size(lerp(start = 24.dp, stop = 28.dp, fraction = fraction))
        )
    }
}