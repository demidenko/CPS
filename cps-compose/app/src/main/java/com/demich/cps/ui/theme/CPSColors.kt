package com.demich.cps.ui.theme

import androidx.compose.material.Colors
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalCPSColors = compositionLocalOf<CPSColors> { throw IllegalAccessError() }

val cpsColors: CPSColors
    @Composable
    @ReadOnlyComposable
    inline get() = LocalCPSColors.current

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
    val warning: Color,
    private val votedRatingNegative: Color,
    val newEntry: Color,
    private val materialInitColors: () -> Colors,
    val ratingColors: CPSRatingColors
): CPSRatingColors by ratingColors {
    fun votedRating(rating: Int): Color =
        if (rating > 0) success else votedRatingNegative

    internal fun materialColors() = materialInitColors().copy(
        background = background,
        surface = background,
        primary = accent,
        onBackground = content,
        error = error
    )
}

internal fun lightCPSColors(usePlatformRatingColors: Boolean) = CPSColors(
    accent = Color(21, 101, 192),
    content = Color(44, 44, 44),
    contentAdditional = Color(131, 131, 131),
    background = Color(248, 248, 248),
    backgroundAdditional = Color(231, 231, 231),
    backgroundNavigation = Color(255, 255, 255),
    divider = Color(85, 85, 85),
    success = Color(0, 128, 0),
    error = Color(200, 0, 32),
    warning = Color(200, 100, 0),
    votedRatingNegative = Color(128, 128, 128),
    newEntry = Color(0xFF669900), //android:color/holo_green_dark,
    materialInitColors = ::lightColors,
    ratingColors = cpsLightThemeRatingColors(usePlatformRatingColors)
)

internal fun darkCPSColors(usePlatformRatingColors: Boolean) = CPSColors(
    accent = Color(0, 153, 204), //android:color/holo_blue_dark
    content = Color(212, 212, 212),
    contentAdditional = Color(147, 147, 147),
    background = Color(18, 18, 18),
    backgroundAdditional = Color(36, 36, 36),
    backgroundNavigation = Color(0, 0, 0),
    divider = Color(85, 85, 85),
    success = Color(51, 153, 51),
    error = Color(210, 64, 64),
    warning = Color(210, 150, 32),
    votedRatingNegative = Color(150, 150, 150),
    newEntry = Color(0xFF99CC00), //android:color/holo_green_light
    materialInitColors = ::darkColors,
    ratingColors = cpsDarkThemeRatingColors(usePlatformRatingColors)
)