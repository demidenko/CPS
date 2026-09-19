package com.demich.cps.utils

import androidx.compose.ui.graphics.Color
import com.demich.cps.ui.theme.CPSColors

enum class WarningLevel {
    SAFE, WARNING, ALERT
}

fun CPSColors.colorFor(warningLevel: WarningLevel): Color =
    when (warningLevel) {
        SAFE -> Color.Unspecified
        WARNING -> warning
        ALERT -> error
    }