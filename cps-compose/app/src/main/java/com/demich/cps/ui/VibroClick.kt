package com.demich.cps.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

@Composable
fun (() -> Unit).withVibration(): () -> Unit {
    val hapticFeedback = LocalHapticFeedback.current
    return remember(key1 = this, key2 = hapticFeedback) {
        {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            invoke()
        }
    }
}

@Composable
fun <T> withVibration(block: (T) -> Unit): (T) -> Unit {
    val hapticFeedback = LocalHapticFeedback.current
    return remember(key1 = block, key2 = hapticFeedback) {
        {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            block(it)
        }
    }
}
