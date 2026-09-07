package com.demich.cps.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.updateTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@Composable
fun <T: Any> AnimatedVisibleByNotNull(
    value: () -> T?,
    modifier: Modifier = Modifier,
    enter: EnterTransition,
    exit: ExitTransition,
    content: @Composable (T) -> Unit
) {
    val value = value()

    val lastNotNullState = remember { mutableStateOf<T?>(null) }.also {
        if (value != null) it.value = value
    }

    val transition = updateTransition(targetState = value, label = null)

    lastNotNullState.value?.let { lastNotNull ->
        val notNullState = lastNotNullState as State<T>
        transition.AnimatedVisibility(
            visible = { it != null },
            modifier = modifier,
            enter = enter,
            exit = exit
        ) {
            content(notNullState.value)
        }
    }
}