package com.demich.cps.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.demich.cps.ui.settingsUI
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberCollect

private val LightColorPalette = lightColors(
    background = Color(248, 248, 248),
    primary = Color(21, 101, 192),
    onBackground = Color(36, 36, 36),
    error = Color(221, 34, 34)
)

private val DarkColorPalette = darkColors(
    background = Color(18, 18, 18),
    primary = Color(0, 153, 204),
    onBackground = Color(212, 212, 212),
    error = Color(200, 64, 64)
)


enum class DarkLightMode {
    DARK, LIGHT, SYSTEM
}

@Composable
private fun DarkLightMode.isDarkMode(): Boolean =
    if (this == DarkLightMode.SYSTEM) isSystemInDarkTheme() else this == DarkLightMode.DARK


val LocalUseOriginalColors = compositionLocalOf { false }

@Composable
fun CPSTheme(content: @Composable () -> Unit) {
    val context = context
    val darkLightMode by rememberCollect { context.settingsUI.darkLightMode.flow }
    val useOriginalColors by rememberCollect { context.settingsUI.useOriginalColors.flow }
    MaterialTheme(
        colors = if (darkLightMode.isDarkMode()) DarkColorPalette else LightColorPalette,
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
            LocalUseOriginalColors provides useOriginalColors,
            LocalContentAlpha provides 1f,
            content = content
        )
    }
}