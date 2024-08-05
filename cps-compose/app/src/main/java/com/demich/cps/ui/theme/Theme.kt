package com.demich.cps.ui.theme

import android.content.Context
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.PlatformTextStyle
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
@ReadOnlyComposable
private fun DarkLightMode.isDarkMode(): Boolean =
    if (this == DarkLightMode.SYSTEM) isSystemInDarkTheme() else this == DarkLightMode.DARK

private fun setSystemBarsStyle(context: Context, isDarkMode: Boolean) {
    val style = if (isDarkMode) SystemBarStyle.dark(Color.TRANSPARENT)
    else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
    (context as ComponentActivity).enableEdgeToEdge(
        statusBarStyle = style,
        navigationBarStyle = style
    )
}

@Composable
private fun ProvideCPSColors(content: @Composable () -> Unit) {
    val context = context
    val darkLightMode by rememberCollect { context.settingsUI.darkLightMode.flow }
    val isDarkMode = darkLightMode.isDarkMode()
    setSystemBarsStyle(context, isDarkMode)
    val useOriginalColors by rememberCollect { context.settingsUI.useOriginalColors.flow }
    val colors = if (isDarkMode) darkCPSColors(useOriginalColors) else lightCPSColors(useOriginalColors)
    CompositionLocalProvider(LocalCPSColors provides colors, content = content)
}

@Composable
fun CPSTheme(content: @Composable () -> Unit) {
    ProvideCPSColors {
        MaterialTheme(
            colors = cpsColors.materialColors(),
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
                LocalContentAlpha provides 1f //because MaterialTheme override
            ) {
                ProvideTextStyle(
                    value = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = true)),
                    content = content
                )
            }
        }
    }
}