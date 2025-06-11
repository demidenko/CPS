package com.demich.cps.ui

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

//TODO: see LocalHapticFeedback in compose 1.8.0

//https://stackoverflow.com/a/70853761

private fun View.vibrate() = reallyPerformHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
private fun View.vibrateStrong() = reallyPerformHapticFeedback(HapticFeedbackConstants.LONG_PRESS)

@Composable
fun (() -> Unit).withVibration(): () -> Unit {
    val view = LocalView.current
    return remember(key1 = this, key2 = view) {
        {
            view.vibrateStrong()
            invoke()
        }
    }
}

@Composable
fun <T> withVibration(block: ((T) -> Unit)): (T) -> Unit {
    val view = LocalView.current
    return remember(key1 = block, key2 = view) {
        {
            view.vibrateStrong()
            block(it)
        }
    }
}

private fun View.reallyPerformHapticFeedback(feedbackConstant: Int) {
    /*if (context.isTouchExplorationEnabled()) {
        // Don't mess with a blind person's vibrations
        return
    }*/
    // Either this needs to be set to true, or android:hapticFeedbackEnabled="true" needs to be set in XML
    isHapticFeedbackEnabled = true

    // Most of the constants are off by default: for example, clicking on a button doesn't cause the phone to vibrate anymore
    // if we still want to access this vibration, we'll have to ignore the global settings on that.
    performHapticFeedback(feedbackConstant, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING)
}
