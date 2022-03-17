package com.demich.cps.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.demich.cps.accounts.managers.HandleColor

private val LightColorPalette = lightColors(
    background = Color(248, 248, 248),
    primary = Color(21, 101, 192),
    onBackground = Color(39, 39, 39),
    error = Color(221, 34, 34)
)

private val DarkColorPalette = darkColors(
    background = Color(18, 18, 18),
    primary = Color(0, 153, 204),
    onBackground = Color(214, 214, 214),
    error = Color(200, 64, 64)
)


object cpsColors {
    //TODO: not optimal? view contains n colors will be recomposed n times?

    private val isLight: Boolean
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colors.isLight

    val colorAccent: Color
        @Composable
        get() = MaterialTheme.colors.primary

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

    val success: Color
        @Composable
        get() = if (isLight) Color(0, 128, 0) else Color(51, 153, 51)

    val errorColor: Color
        @Composable
        get() = MaterialTheme.colors.error

    @Composable
    fun handleColor(handleColor: HandleColor): Color {
        if (isLight) {
            return when(handleColor) {
                HandleColor.GRAY -> Color(0xFF808080)
                HandleColor.BROWN -> Color(0xFF804000)
                HandleColor.GREEN -> Color(0xFF008000)
                HandleColor.CYAN -> Color(0xFF03A89E)
                HandleColor.BLUE -> Color(0xFF0000FF)
                HandleColor.VIOLET -> Color(0xFFAA00AA)
                HandleColor.YELLOW -> Color(0xFFDDC000)
                HandleColor.ORANGE -> Color(0xFFFF8000)
                HandleColor.RED -> Color(0xFFFF0000)
            }
        } else {
            return when(handleColor) {
                HandleColor.GRAY -> Color(0xFF888888)
                HandleColor.BROWN -> Color(0xFF80461B)
                HandleColor.GREEN -> Color(0xFF009000)
                HandleColor.CYAN -> Color(0xFF00A89E)
                HandleColor.BLUE -> Color(0xFF3F68F0)
                HandleColor.VIOLET -> Color(0xFFB04ECC)
                HandleColor.YELLOW -> Color(0xFFCCCC00)
                HandleColor.ORANGE -> Color(0xFFFB8000)
                HandleColor.RED -> Color(0xFFED301D)
            }
        }
    }
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