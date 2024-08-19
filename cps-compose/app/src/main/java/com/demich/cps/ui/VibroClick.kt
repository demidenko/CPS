package com.demich.cps.ui

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalView

//https://stackoverflow.com/a/70853761

private fun View.vibrate() = reallyPerformHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
private fun View.vibrateStrong() = reallyPerformHapticFeedback(HapticFeedbackConstants.LONG_PRESS)

@Composable
@ReadOnlyComposable
fun (() -> Unit).withVibration(): () -> Unit {
    val view = LocalView.current
    return {
        view.vibrateStrong()
        invoke()
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
