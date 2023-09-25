package com.demich.cps.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.demich.cps.ui.theme.cpsColors

enum class DangerType {
    SAFE, WARNING, DANGER
}

@Composable
@ReadOnlyComposable
fun colorFor(dangerType: DangerType): Color =
    when (dangerType) {
        DangerType.SAFE -> Color.Unspecified
        DangerType.WARNING -> cpsColors.warning
        DangerType.DANGER -> cpsColors.error
    }