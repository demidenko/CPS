package com.demich.cps.utils

import androidx.compose.ui.graphics.Color
import com.demich.cps.ui.theme.CPSColors

enum class WarningLevel {
    WARNING, ALERT
}

fun CPSColors.colorFor(warningLevel: WarningLevel): Color =
    when (warningLevel) {
        WARNING -> warning
        ALERT -> error
    }