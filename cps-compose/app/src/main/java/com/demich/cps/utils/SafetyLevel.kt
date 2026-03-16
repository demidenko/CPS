package com.demich.cps.utils

import androidx.compose.ui.graphics.Color
import com.demich.cps.ui.theme.CPSColors

enum class SafetyLevel {
    SAFE, WARNING, DANGER
}

fun CPSColors.colorFor(safetyLevel: SafetyLevel): Color =
    when (safetyLevel) {
        SAFE -> Color.Unspecified
        WARNING -> warning
        DANGER -> error
    }