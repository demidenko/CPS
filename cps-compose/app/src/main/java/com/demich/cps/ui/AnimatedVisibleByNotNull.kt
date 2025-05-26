package com.demich.cps.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.updateTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier

@Composable
fun<T: Any> AnimatedVisibleByNotNull(
    value: () -> T?,
    modifier: Modifier = Modifier,
    enter: EnterTransition,
    exit: ExitTransition,
    content: @Composable (T) -> Unit
) {
    val state = remember { mutableStateOf<T?>(null) }
    val v = value()?.also { state.value = it }

    val transition = updateTransition(targetState = v, label = null)

    state.value?.let { lastNotNull ->
        val notNullState = rememberUpdatedState(newValue = lastNotNull)
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