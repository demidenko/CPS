package com.demich.cps.ui

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object CPSDefaults {
    val topBarHeight get() = 56.dp
    val tabsRowHeight get() = 45.dp

    val bottomBarHeight get() = 56.dp //as BottomNavigationHeight

    val scrollBarWidth get() = 5.dp

    val MonospaceTextStyle
        get() = TextStyle(
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.1.sp
        )

    @Stable
    fun <T> toggleAnimationSpec(): AnimationSpec<T> =
        tween(durationMillis = 800)
}

object CPSFontSize {
    val settingsTitle get() = 18.sp
    val settingsSubtitle get() = 15.sp
    val settingsDescription get() = 14.sp
    val settingsSectionTitle get() = 14.sp
}