package com.demich.cps.ui.theme

import androidx.compose.ui.graphics.Color
import com.demich.cps.profiles.RatingColor

interface CPSRatingColors {
    fun ratingColor(ratingColor: RatingColor): Color

    val usePlatformRatingColors: Boolean
}

fun cpsLightThemeRatingColors(usePlatformRatingColors: Boolean): CPSRatingColors =
    object : CPSRatingColors {
        override fun ratingColor(ratingColor: RatingColor) =
            when (ratingColor) {
                GRAY -> Color(0xFF808080)
                BROWN -> Color(0xFF804000)
                GREEN -> Color(0xFF008000)
                CYAN -> Color(0xFF03A89E)
                BLUE -> Color(0xFF0000FF)
                VIOLET -> Color(0xFFAA00AA)
                YELLOW -> Color(0xFFDDC000)
                ORANGE -> Color(0xFFFF8000)
                RED -> Color(0xFFFF0000)
            }

        override val usePlatformRatingColors = usePlatformRatingColors
    }

fun cpsDarkThemeRatingColors(usePlatformRatingColors: Boolean): CPSRatingColors =
    object : CPSRatingColors {
        override fun ratingColor(ratingColor: RatingColor) =
            when (ratingColor) {
                GRAY -> Color(0xFF888888)
                BROWN -> Color(0xFF80461B)
                GREEN -> Color(0xFF009600)
                CYAN -> Color(0xFF00A89E)
                BLUE -> Color(0xFF0F68F0) // TODO: bad contrast https://webaim.org/resources/contrastchecker/?fcolor=0F68F0&bcolor=121212
                VIOLET -> Color(0xFFBB4ECC)
                YELLOW -> Color(0xFFCCCC00)
                ORANGE -> Color(0xFFFB8000)
                RED -> Color(0xFFED301D)
            }

        override val usePlatformRatingColors = usePlatformRatingColors
    }
