package com.demich.cps.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.demich.cps.ui.settingsUI
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberCollect


enum class DarkLightMode {
    DARK, LIGHT, SYSTEM
}

@Composable
private fun DarkLightMode.isDarkMode(): Boolean =
    if (this == DarkLightMode.SYSTEM) isSystemInDarkTheme() else this == DarkLightMode.DARK

@Composable
fun CPSTheme(content: @Composable () -> Unit) {
    val context = context
    val darkLightMode by rememberCollect { context.settingsUI.darkLightMode.flow }
    val useOriginalColors by rememberCollect { context.settingsUI.useOriginalColors.flow }
    val colors = if (darkLightMode.isDarkMode()) darkCPSColors(useOriginalColors) else lightCPSColors(useOriginalColors)
    MaterialTheme(
        colors = colors.materialColors,
        typography = Typography(
            body1 = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                letterSpacing = 0.3.sp
            )
        )
    ) {
        CompositionLocalProvider(
            LocalCPSColors provides colors,
            LocalContentAlpha provides 1f,
            content = content
        )
    }
}