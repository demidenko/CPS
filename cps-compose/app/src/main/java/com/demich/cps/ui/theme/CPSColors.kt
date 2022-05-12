package com.demich.cps.ui.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.demich.cps.accounts.managers.HandleColor

object cpsColors {
    //TODO: object with vals instead of getters

    private val isLight: Boolean
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colors.isLight

    val accent: Color
        @Composable
        get() = MaterialTheme.colors.primary

    val textColor: Color
        @Composable
        get() = MaterialTheme.colors.onBackground

    val contentAdditional: Color
        @Composable
        get() = if (isLight) Color(118, 118, 118) else Color(147, 147, 147)

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

    val votedRatingPositive: Color
        @Composable
        get() = success

    val votedRatingNegative: Color
        @Composable
        get() = if (isLight) Color(128, 128, 128) else Color(150, 150, 150)

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
                HandleColor.BLUE -> Color(0xFF0F68F0)
                HandleColor.VIOLET -> Color(0xFFB04ECC)
                HandleColor.YELLOW -> Color(0xFFCCCC00)
                HandleColor.ORANGE -> Color(0xFFFB8000)
                HandleColor.RED -> Color(0xFFED301D)
            }
        }
    }
}