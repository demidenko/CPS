package com.demich.cps.ui.theme

import androidx.compose.material.Colors
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import com.demich.cps.accounts.managers.HandleColor

internal val LocalCPSColors = compositionLocalOf<CPSColors> { throw IllegalAccessError() }

val cpsColors: CPSColors
    @Composable
    @ReadOnlyComposable
    get() = LocalCPSColors.current

class CPSColors(
    val accent: Color,
    val content: Color,
    val contentAdditional: Color,
    val background: Color,
    val backgroundAdditional: Color,
    val backgroundNavigation: Color,
    val divider: Color,
    val success: Color,
    val error: Color,
    private val votedRatingNegative: Color,
    val newEntry: Color,
    materialInitColors: () -> Colors,
    handleColor: (HandleColor) -> Color
) {
    fun votedRating(rating: Int): Color =
        if (rating > 0) success else votedRatingNegative

    private val handleColors = HandleColor.values().map(handleColor)
    fun handleColor(handleColor: HandleColor): Color = handleColors[handleColor.ordinal]

    internal val materialColors = materialInitColors().copy(
        background = background,
        primary = accent,
        onBackground = content,
        error = error
    )
}

internal val lightCPSColors = CPSColors(
    accent = Color(21, 101, 192),
    content = Color(36, 36, 36),
    contentAdditional = Color(118, 118, 118),
    background = Color(248, 248, 248),
    backgroundAdditional = Color(234, 234, 234),
    backgroundNavigation = Color(255, 255, 255),
    divider = Color(85, 85, 85),
    success = Color(0, 128, 0),
    error = Color(221, 34, 34),
    votedRatingNegative = Color(128, 128, 128),
    newEntry = Color(0xFF669900), //android:color/holo_green_dark
    materialInitColors = ::lightColors
) {
    when (it) {
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
}

internal val darkCPSColors = CPSColors(
    accent = Color(0, 153, 204), //android:color/holo_blue_dark
    content = Color(212, 212, 212),
    contentAdditional = Color(147, 147, 147),
    background = Color(18, 18, 18),
    backgroundAdditional = Color(36, 36, 36),
    backgroundNavigation = Color(0, 0, 0),
    divider = Color(85, 85, 85),
    success = Color(51, 153, 51),
    error = Color(200, 64, 64),
    votedRatingNegative = Color(150, 150, 150),
    newEntry = Color(0xFF99CC00), //android:color/holo_green_light
    materialInitColors = ::darkColors
) {
    when (it) {
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