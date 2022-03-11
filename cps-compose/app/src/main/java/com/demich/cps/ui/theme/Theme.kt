package com.demich.cps.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

private val LightColorPalette = lightColors(
    background = Color(248, 248, 248),
    secondary = Color(21, 101, 192),
    onBackground = Color(39, 39, 39)
)

private val DarkColorPalette = darkColors(
    background = Color(18, 18, 18),
    secondary = Color(0, 153, 204),
    onBackground = Color(214, 214, 214),
    onSurface = Color.Red
)


object cpsColors {
    //TODO: not optimal? view contains n colors will be recomposed n times?

    private val isLight: Boolean
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colors.isLight

    val colorAccent: Color
        @Composable
        get() = MaterialTheme.colors.secondary

    val textColor: Color
        @Composable
        get() = MaterialTheme.colors.onBackground

    val textColorAdditional: Color
        @Composable
        get() = if (isLight) Color(133, 133, 133) else Color(147, 147, 147)

    val background: Color
        @Composable
        get() = MaterialTheme.colors.background

    val backgroundAdditional: Color
        @Composable
        get() = if (isLight) Color(230, 230, 230) else Color(34, 34, 34)

    val backgroundNavigation: Color
        @Composable
        get() = if (isLight) Color(255, 255, 255) else Color(0, 0, 0)

    val dividerColor: Color
        //@Composable
        get() = Color(85, 85, 85)
}


@Composable
fun CPSTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colors = if (darkTheme) DarkColorPalette else LightColorPalette,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}