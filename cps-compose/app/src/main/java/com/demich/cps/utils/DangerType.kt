package com.demich.cps.utils

import androidx.compose.ui.graphics.Color
import com.demich.cps.ui.theme.CPSColors

enum class DangerType {
    SAFE, WARNING, DANGER
}

fun CPSColors.colorFor(dangerType: DangerType): Color =
    when (dangerType) {
        SAFE -> Color.Unspecified
        WARNING -> warning
        DANGER -> error
    }